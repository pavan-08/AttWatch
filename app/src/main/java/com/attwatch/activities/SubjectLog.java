package com.attwatch.activities;

import android.os.Build;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.attwatch.R;
import com.attwatch.adapters.LogPagerAdapter;

public class SubjectLog extends AppCompatActivity {

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.material_indigo_700));
        }
        setContentView(R.layout.activity_subject_log);

        toolbar = (Toolbar) findViewById(R.id.log_toolbar);
        tabLayout = (TabLayout) findViewById(R.id.log_tab_layout);
        viewPager = (ViewPager)findViewById(R.id.log_view_pager);

        toolbar.setTitle(getIntent().getStringExtra("subjectName"));
        viewPager.setAdapter(new LogPagerAdapter(getSupportFragmentManager(), getIntent().getIntExtra("subjectID", 1)));
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(this, R.color.white));
        tabLayout.setTabTextColors(ContextCompat.getColorStateList(this, R.color.selector));

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
