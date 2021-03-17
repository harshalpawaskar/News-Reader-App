package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> title = new ArrayList<>();
    static ArrayList<String> content = new ArrayList<>();
    ListView listView;
    ArrayAdapter arrayAdapter;
    SQLiteDatabase sqLiteDatabase;

    public void UpdateList()
    {
        Cursor c = sqLiteDatabase.rawQuery("SELECT * FROM news",null);

        int titleIndex = c.getColumnIndex("title");
        int contentIndex = c.getColumnIndex("content");

        if(c.moveToFirst())
        {
            title.clear();
            content.clear();

            do {
                title.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));

                c.moveToNext();
            }while (!c.isAfterLast());
        }

        arrayAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sqLiteDatabase = this.openOrCreateDatabase("NewsReader",MODE_PRIVATE,null);
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS news (id INTEGER PRIMARY KEY,articleID INTEGER,title VARCHAR,content VARCHAR)");

        listView = (ListView) findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1,title);
        listView.setAdapter(arrayAdapter);

        DownloadTask downloadTask = new DownloadTask();
        try {
            //downloadTask.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }catch (Exception e)
        {
            e.printStackTrace();
        }

        UpdateList();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), NewsActivity.class);
                intent.putExtra("content",content.get(i));
                startActivity(intent);
            }
        });
    }

    public class DownloadTask extends AsyncTask<String,Void,String>{

        @Override
        protected String doInBackground(String... urls) {
            URL url;
            HttpURLConnection httpURLConnection = null;

            try {
                url = new URL(urls[0]);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = httpURLConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(inputStream);
                int data = reader.read();
                String result = "";

                while (data!=-1)
                {
                    result+=(char) data;
                    data = reader.read();
                }

                JSONArray jsonArray = new JSONArray(result);
                int newsLength = 50;
                if(jsonArray.length()<50)
                    newsLength = jsonArray.length();

                sqLiteDatabase.execSQL("DELETE FROM news");

                for(int i=0;i<newsLength;i++)
                {
                    String apiID = jsonArray.getString(i);

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+ apiID +".json?print=pretty");
                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    inputStream = httpURLConnection.getInputStream();
                    reader = new InputStreamReader(inputStream);
                    data = reader.read();
                    String jsonContent = "";

                    while (data!=-1)
                    {
                        jsonContent+=(char) data;
                        data = reader.read();
                    }

                    JSONObject jsonObject = new JSONObject(jsonContent);
                    String newsTitle = jsonObject.getString("title");
                    String newsUrl = "";
                    try {
                        newsUrl = jsonObject.getString("url");
                    }catch (Exception e)
                    {
                        continue;
                    }

                    if(!newsTitle.equals("") && !newsUrl.equals(""))
                    {
                        /*url = new URL(newsUrl);
                        httpURLConnection = (HttpURLConnection) url.openConnection();
                        inputStream = httpURLConnection.getInputStream();
                        reader = new InputStreamReader(inputStream);
                        data = reader.read();
                        String htmlContent = "";

                        while (data!=-1)
                        {
                            htmlContent+=(char) data;
                            data = reader.read();
                        }*/

                        String sql = "INSERT INTO news(articleID,title,content) VALUES (?,?,?)";
                        SQLiteStatement sqLiteStatement = sqLiteDatabase.compileStatement(sql);
                        sqLiteStatement.bindString(1,apiID);
                        sqLiteStatement.bindString(2,newsTitle);
                        //sqLiteStatement.bindString(3,htmlContent);
                        sqLiteStatement.bindString(3,newsUrl);

                        sqLiteStatement.execute();

                        //Log.i("Title and Content",newsTitle + " " + htmlContent);
                    }
                }

                Log.i("Result",result);

            }catch (Exception e)
            {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            UpdateList();
        }
    }
}