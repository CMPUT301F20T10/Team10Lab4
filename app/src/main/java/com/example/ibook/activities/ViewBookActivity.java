package com.example.ibook.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ibook.R;
import com.example.ibook.entities.Book;
import com.example.ibook.entities.BookRequest;
import com.example.ibook.entities.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.FileInputStream;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ViewBookActivity extends AppCompatActivity {
    private String userID;
    private ArrayList<BookRequest> requests;
    private String bookID;
    private final int REQ_CAMERA_IMAGE = 1;
    private final int REQ_GALLERY_IMAGE = 2;

    private TextView bookNameTextView;
    private TextView authorTextView;
    private TextView dateTextView;
    private TextView isbnTextView;
    private TextView descriptionTextView;
    private ImageView imageView;

    private TextView edit_button;
    private Button backButton;
    private Button delete_button;
    private Button request_button;
    private Button return_button;
    private ListView requestList;

    private FirebaseFirestore db;
    FirebaseAuth uAuth;
    private Book selectedBook;
    private String owner;
    private String status;

    public static User requestReceiver;
    private User currentUser;

    private boolean isRelated = false;
    private static boolean imageChanged;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //Hide the top bar and make it full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE); //will hide the title
        getSupportActionBar().hide(); // hide the title bar
        setContentView(R.layout.activity_view_book);

        bookNameTextView = findViewById(R.id.ViewBookName);
        authorTextView = findViewById(R.id.ViewAuthor);
        dateTextView = findViewById(R.id.ViewDate);
        isbnTextView = findViewById(R.id.ViewISBN);
        descriptionTextView = findViewById(R.id.descriptionView2);
        imageView = findViewById(R.id.imageView);

        edit_button = findViewById(R.id.editButton);
        request_button = findViewById(R.id.btn_request_book);
        backButton = findViewById(R.id.cancelButton);
        delete_button = findViewById(R.id.btn_delete_book);
        return_button = findViewById(R.id.btn_return_book);

        imageChanged = false;
        requestList = findViewById(R.id.request_list);
        requests = new ArrayList<BookRequest>();
        final ArrayAdapter requestAdapter = new ArrayAdapter<>(getBaseContext(), R.layout.request_list_content, R.id.request_content, requests);
        requestList.setAdapter(requestAdapter);

        uAuth = FirebaseAuth.getInstance();
        userID = uAuth.getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();
        final DocumentReference docRef = db.collection("users").document(userID);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                currentUser = documentSnapshot.toObject(User.class);
            }
        });

        Intent intent = getIntent();
        bookID = intent.getStringExtra("BOOK_ID");
        owner = intent.getStringExtra("OWNER");
        status = intent.getStringExtra("STATUS");

        getBookData();
        checkCases();

        edit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewBookActivity.this, EditBookActivity.class);
                intent.putExtra("BOOK_ID", bookID);
                startActivityForResult(intent, 3);
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        return_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //BookRequest newRequest = new BookRequest(currentUser.getUserID(),requestReceiver.getUserID(),selectedBook.getBookID(), "Requested");
                //db.collection("bookRequest").document().set(newRequest);

                MainActivity.database
                        .getDb()
                        .collection("bookRequest")
                        .whereEqualTo("requestSenderID", userID)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                    if (!((String) documentSnapshot.get("requestedBookID")).equals(bookID)) {
                                        continue; // continue if not this book
                                    }
                                    BookRequest newRequest = documentSnapshot.toObject(BookRequest.class);
                                    // todo: so far, no need to change request status

                                    final DocumentReference docRef = db.collection("users").document(newRequest.getRequestReceiverID());

                                    docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            requestReceiver = documentSnapshot.toObject(User.class);
                                            requestReceiver.addToNotificationList(currentUser.getUserName() + " wants to return your book " + selectedBook.getTitle());
                                            docRef.set(requestReceiver);

                                            Toast.makeText(getBaseContext(), "raised a return request", Toast.LENGTH_SHORT).show();
                                            finish();
                                        }
                                    });


                                }
                            }
                        });


                // Q: finish the activity or not?
                //finish();
            }
        });

        request_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                // add the book to requested list


                final DocumentReference docRefRequestReceiver = db.collection("users").document(owner);

                docRefRequestReceiver.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        requestReceiver = documentSnapshot.toObject(User.class);
                        requestReceiver.addToNotificationList(currentUser.getUserName() + " wants to borrow your book " + selectedBook.getTitle());
                        //updating notificaion list of the user in database
                        docRefRequestReceiver.set(requestReceiver);
                        Toast.makeText(getBaseContext(), "Coming here!", Toast.LENGTH_SHORT).show();

                        // three requestStatus: Requested, Accepted, Confirmed
                        BookRequest newRequest = new BookRequest(currentUser.getUserID(), requestReceiver.getUserID(), selectedBook.getBookID(), "Requested");
                        db.collection("bookRequest").document().set(newRequest);

                        //change book status
                        System.out.println("Selected bookID: " + selectedBook.getBookID());

                        selectedBook.setStatus(Book.Status.Requested);

                        final DocumentReference bookRef = db.collection("books").document(selectedBook.getBookID());
                        bookRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                bookRef.set(selectedBook);
                                //TODO: Update the status of the book in the user collection bookList, the book collection has owner ID so you can use that to go to user collection
                                //TODO: and update his booklists' book status

                                //maybe don't have to do this if we are always using the book collection and bookRequestCollection but still something to think about

                            }
                        });
                    }
                });


                System.out.println("Coming before db");
                db.collection("users").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                    }
                });

                Toast.makeText(getBaseContext(), "This function is coming soon!", Toast.LENGTH_SHORT).show();

            }//onClick
        });

        // setting up the request list
        final CollectionReference requestRef = db.collection("bookRequest");
        requestRef
                .whereEqualTo("requestedBookID", bookID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String sender = document.getString("requestSenderID");
                            db.collection("users")
                                    .document(sender)
                                    .get()
                                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            User sender = documentSnapshot.toObject(User.class);
                                            requestAdapter.add(sender.getUserName() + " has requested this book");
                                        }
                                    });
                        }
                    }
                });
    }

    public void delete_book(View view) {
        MainActivity.database.deleteImage(selectedBook.getBookID());
        db.collection("books").document(selectedBook.getBookID())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        db.collection("bookRequest")
                                .whereEqualTo("requestedBookID", selectedBook.getBookID())
                                .get()
                                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                            documentSnapshot.getReference().delete();
                                        }
                                    }
                                });
                    }
                });
        finish();
    }


    /**
     * This method will be invoked when the user's focus comes back to ViewBookActivity
     * It will refresh the data from the database, so that if any data was updated, they will be displayed correctly
     */
    @Override
    protected void onResume() {
        super.onResume();
        // get data again when resume

        //getBookData();
    }

    /**
     * Get the results from Edit book so we can display the proper image.
     * This is because the database is asynchronous so it won't upload the image in time
     * for us to download. So we need to pass the bitmap through the intent.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == 4 && requestCode == 3) {
            SystemClock.sleep(500);
            //Set new image if it changed.
            if (data.getExtras() != null) {
                try {
                    Log.i("image", "changed");
                    String tempFileName = data.getStringExtra("CHANGED_IMAGE");
                    FileInputStream is = this.openFileInput(tempFileName);
                    Bitmap new_pic = BitmapFactory.decodeStream(is);
                    imageView = findViewById(R.id.imageView);
                    imageView = EditBookActivity.scaleAndSetImage(new_pic, imageView);
                    is.close();
                    imageChanged = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            getBookData();
        }
    }

    /**
     * This method will retrieve the data from the database,
     * and assign the data to the TextViews, so that they are displayed correctly.
     */
    private void getBookData() {
        // if it's not owner's book, we cannot access the book from user
        // so find the book from book collection

        db.collection("books").document(bookID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        selectedBook = documentSnapshot.toObject(Book.class);

                        bookNameTextView.setText(selectedBook.getTitle());
                        authorTextView.setText(selectedBook.getAuthors());
                        dateTextView.setText(selectedBook.getDate());
                        isbnTextView.setText(selectedBook.getIsbn());
                        if (selectedBook.getDescription() != null) {
                            descriptionTextView.setText(selectedBook.getDescription());
                        }
                        if (!imageChanged) {
                            MainActivity.database.downloadImage(imageView, selectedBook.getBookID(), true);
                        }
                        imageChanged = false;
                    }
                });
    }

    /**
     * This method will check whether the current user is the owner of the book
     * and then set the UIs accordingly.
     */
    private void checkCases() {

        final Book.Status bookStatus = Book.Status.valueOf(status);

        // owner
        if (userID.equals(owner)) {
            if (bookStatus.equals(Book.Status.Available) || bookStatus.equals(Book.Status.Requested)) {
                // if owner & book available/requested, edit allowed
                request_button.setVisibility(View.GONE);
                return_button.setVisibility(View.GONE);
            }
            // else if() //if owner & book accepted, nothing allowed
            // todo: later can show some information to let the owner know it's accepted
            else {
                // if owner & book accepted/borrowed, nothing allowed
                edit_button.setVisibility(View.GONE);
                delete_button.setVisibility(View.GONE);
                request_button.setVisibility(View.GONE);
                requestList.setVisibility(View.GONE);
                return_button.setVisibility(View.GONE);
            }
        } else {
            isRelated = false;
            MainActivity.database
                    .getDb()
                    .collection("bookRequest")
                    .whereEqualTo("requestSenderID", userID)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                if (!((String) documentSnapshot.get("requestedBookID")).equals(bookID)) {
                                    continue; // continue if not this book
                                }
                                if (documentSnapshot.contains("requestStatus")) {
                                    // already request the book
                                    if (((String) documentSnapshot.get("requestStatus")).equals("Requested")) {
                                        edit_button.setVisibility(View.GONE);
                                        delete_button.setVisibility(View.GONE);
                                        request_button.setVisibility(View.GONE);
                                        requestList.setVisibility(View.GONE);
                                        return_button.setVisibility(View.GONE);
                                        Toast.makeText(getBaseContext(), "Canceling requests to be done", Toast.LENGTH_SHORT).show();
                                    } else if (((String) documentSnapshot.get("requestStatus")).equals("Accepted")) {

                                        // todo: launch an activity with scanning to confirm it
                                        Toast.makeText(getBaseContext(), "launch an activity with scanning to confirm it", Toast.LENGTH_SHORT).show();
                                        edit_button.setVisibility(View.GONE);
                                        delete_button.setVisibility(View.GONE);
                                        request_button.setVisibility(View.GONE);
                                        requestList.setVisibility(View.GONE);
                                        return_button.setVisibility(View.GONE);

                                    } else if (((String) documentSnapshot.get("requestStatus")).equals("Confirmed")) {
                                        // may want to return the book
                                        edit_button.setVisibility(View.GONE);
                                        delete_button.setVisibility(View.GONE);
                                        request_button.setVisibility(View.GONE);
                                        requestList.setVisibility(View.GONE);
                                    }
                                    isRelated = true;
                                    break;
                                } else {
                                    Toast.makeText(getBaseContext(), "wrong request format", Toast.LENGTH_SHORT).show();
                                }

                            }
                            if (!isRelated) {
                                if (bookStatus.equals(Book.Status.Available) || bookStatus.equals(Book.Status.Requested)) {
                                    // if non-owner & book available/requested, request allowed
                                    edit_button.setVisibility(View.GONE);
                                    delete_button.setVisibility(View.GONE);
                                    requestList.setVisibility(View.GONE);
                                    return_button.setVisibility(View.GONE);
                                } else {
                                    // nothing can do
                                    edit_button.setVisibility(View.GONE);
                                    delete_button.setVisibility(View.GONE);
                                    request_button.setVisibility(View.GONE);
                                    requestList.setVisibility(View.GONE);
                                    return_button.setVisibility(View.GONE);
                                }
                            }
                        }
                    });


        }
    }

}