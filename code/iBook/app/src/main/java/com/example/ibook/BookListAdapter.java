package com.example.ibook;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class BookListAdapter extends BaseAdapter {
    private ArrayList<Book> books;
    private Context context;
    private TextView title;
    private TextView authors;
    private TextView date;
    private TextView description;
    private TextView status;
    //Get image view

    public BookListAdapter(ArrayList<Book> books, Context context) {
        this.books = books;
        this.context = context;
    }

    @Override
    public int getCount() {
        return books.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView==null){
            convertView= LayoutInflater.from(context).inflate(R.layout.book_list_content, parent, false);
        }
        //Get the current book
        Book book = books.get(position);
        //Get the xml attributes
        title = convertView.findViewById(R.id.listBookTitle);
        authors = convertView.findViewById(R.id.listBookAuthors);
        date = convertView.findViewById(R.id.listBookDate);
        description = convertView.findViewById(R.id.listBookDescription);
        status = convertView.findViewById(R.id.listBookStatus);
        //Get the image attribute

        //Set the values for the xml attributes
        title.setText(book.getTitle());
        authors.setText(book.getAuthor());
        date.setText(book.getDate());

        //Set part of the description up to ~30 characters
        description.setText(book.getDescription().substring(0,30) + "...");
        if(book.isAvailable()){
            status.setText("Status: Available");
            status.setTextColor(0xFF1E9F01);
        }else{
            status.setText("Status: " + book.getState());
            status.setTextColor(0xFFFF0000);
        }

        //Set the image if there is one

        return convertView;
    }
}