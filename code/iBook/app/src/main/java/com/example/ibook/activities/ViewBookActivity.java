package com.example.ibook.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.ibook.R;
import com.example.ibook.entities.*;
import com.example.ibook.fragment.EditBookFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.w3c.dom.Document;

public class ViewBookActivity extends AppCompatActivity implements EditBookFragment.OnFragmentInteractionListener{
    private String userID;
    private Book book;
    private int bookNumber;

    private TextView bookNameTextView;
    private TextView authorTextView;
    private TextView dateTextView;
    private TextView isbnTextView;
    private ImageView imageView;
    private Button edit_button;
    //private Button delete_button;
    private FirebaseFirestore db;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        userID = intent.getStringExtra("USER_ID");
        bookNumber = intent.getIntExtra("BOOK_NUMBER", 0);
        // Toast.makeText(getBaseContext(), String.valueOf(bookNumber), Toast.LENGTH_SHORT).show();
        // Toast.makeText(getBaseContext(), userID, Toast.LENGTH_SHORT).show();
        db = FirebaseFirestore.getInstance();

        User user = new com.example.ibook.entities.User();
        DocumentReference docRef = user.getDocumentReference();
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        ArrayList<Book> hashList = (ArrayList<Book>) document.get("BookList");
                        Map<String, Object> convertMap = (Map<String, Object>) hashList.get(bookNumber);
                        book = new Book(
                                String.valueOf(convertMap.get("title")),
                                String.valueOf(convertMap.get("author")),
                                String.valueOf(convertMap.get("date")),
                                (String.valueOf(convertMap.get("description"))),
                                //from_string_to_enum(String.valueOf(convertMap.get("status"))),
                                Book.Status.Available,
                                String.valueOf(convertMap.get("isbn"))
                        );
                        setContentView(R.layout.activity_view_book);

                        bookNameTextView = findViewById(R.id.ViewBookName);
                        authorTextView = findViewById(R.id.ViewAuthor);
                        dateTextView = findViewById(R.id.ViewDate);
                        isbnTextView = findViewById(R.id.ViewISBN);

                        bookNameTextView.setText(book.getTitle());
                        authorTextView.setText(book.getAuthor());
                        dateTextView.setText(book.getDate());
                        isbnTextView.setText(book.getIsbn());


                    } else {
                        //Log.d(TAG, "No such document");
                    }
                } else {
                    //Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });


        //TODO !: imageView

        //TODO 2: ViewBookActivity with 4 kinds of interaction

        //TODO 3: ViewBookActivity conducted by the owner of the book
    }


    public void edit_book(View view){

        new EditBookFragment(userID, bookNumber).show(getSupportFragmentManager(), "Edit_Book");
        // todo: receive response
    }

    @Override
    public void onOkPressed(boolean isChanged, final Book book){
        if(isChanged){
            db = FirebaseFirestore.getInstance();
            DocumentReference docRef = db.collection("users").document(userID);
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {

                            bookNameTextView = findViewById(R.id.ViewBookName);
                            authorTextView = findViewById(R.id.ViewAuthor);
                            dateTextView = findViewById(R.id.ViewDate);
                            isbnTextView = findViewById(R.id.ViewISBN);

                            bookNameTextView.setText(book.getTitle());
                            authorTextView.setText(book.getAuthor());
                            dateTextView.setText(book.getDate());
                            isbnTextView.setText(book.getIsbn());
                            Intent intent = new Intent();
                            setResult(1, intent);

                        } else {
                            //Log.d(TAG, "No such document");
                        }
                    } else {
                        //Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });
        }
    }

    public void delete_book(View view) {
        DocumentReference docRef = db.collection("users").document(userID);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Map<String, Object> data;
                        data = document.getData();
                        ArrayList<Book> books = (ArrayList<Book>) document.getData().get("BookList");
                        books.remove(bookNumber);
                        data.put("BookList", books);
                        db.collection("users")
                                .document(userID).set(data);
                        Intent intent = new Intent();
                        setResult(1, intent);
                        finish();
                    } else {
                        //Log.d(TAG, "No such document");
                    }
                } else {
                    //Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }
}
