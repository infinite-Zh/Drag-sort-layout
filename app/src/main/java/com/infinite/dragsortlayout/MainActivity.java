package com.infinite.dragsortlayout;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private DragSortLayout mLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLayout= (DragSortLayout) findViewById(R.id.dragLayout);
        mLayout.setOnItemClickListener(new DragSortLayout.OnItemClickListener() {
            @Override
            public void onItemClick(View childView, int position) {
                if (childView instanceof TextView)
                Log.e("click",((TextView)childView).getText()+"   "+position);
            }
        });
    }
}
