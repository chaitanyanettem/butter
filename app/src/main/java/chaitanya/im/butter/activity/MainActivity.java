package chaitanya.im.butter.activity;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import java.util.ArrayList;

import chaitanya.im.butter.APICall;
import chaitanya.im.butter.Adapters.PosterGridAdapter;
import chaitanya.im.butter.Data.GridDataModel;
import chaitanya.im.butter.EndlessScrollListener;
import chaitanya.im.butter.R;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static RecyclerView.Adapter adapter;
    private GridLayoutManager layoutManager;
    private static RecyclerView moviePosters;
    private static ArrayList<GridDataModel> data;
    private static final String BASE_URL = "http://api.themoviedb.org/3/";
    private String endpoint = "popular";
    private static APICall popularMovies;
    private int currentNavSelection = R.id.nav_popular;
    public final static String TAG = "MainActivity.java";
    private EndlessScrollListener mEndlessScrollListener;
    private String activity_title = "Popular";
    public static BottomSheetBehavior bottomSheetBehavior;
    public static String trailerURL;
    private CoordinatorLayout coordinatorLayout;
    public static SwipeRefreshLayout swipeRefreshLayout;
    int bottomSheetState=-1;
    int posterW = 0;
    int spanCount = 0;
    AppCompatActivity activity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        moviePosters = (RecyclerView) findViewById(R.id.movie_posters);
        moviePosters.setHasFixedSize(true);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                R.color.darkBackgroundMain,
                R.color.rippleColor);

        getSpanAndPosterW();

        layoutManager = new GridLayoutManager(this, spanCount);
        moviePosters.setLayoutManager(layoutManager);
        moviePosters.setItemAnimator(new DefaultItemAnimator());

        popularMovies = new APICall(BASE_URL, endpoint, posterW, null, true, activity);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        data = new ArrayList<>();
        adapter = new PosterGridAdapter(data, this, posterW, this);
        moviePosters.setAdapter(adapter);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        View bottomSheet = findViewById(R.id.bottomSheet);
        coordinatorLayout = (CoordinatorLayout) bottomSheet;

        //for bottomsheet to appear above toolbar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            coordinatorLayout.setElevation(R.dimen.appbar_elevation);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(currentNavSelection);
        setTitle(activity_title);

        mEndlessScrollListener = new EndlessScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount) {
                Log.d(MainActivity.TAG, "Load MOAR THINGS!!!");
                popularMovies = new APICall(BASE_URL, endpoint, posterW, page, false, activity);
            }
        };

        moviePosters.addOnScrollListener(mEndlessScrollListener);

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetState = 1;
                    Log.d(TAG, "BottomSheet Expanded.");
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetState = -1;
                    Log.d(TAG, "BottomSheet Collapsed.");
                    //getSupportActionBar().show();
                    MainActivity.bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                } else if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    bottomSheetState = -1;
                    Log.d(TAG, "BottomSheet Hidden.");
                } else if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    bottomSheetState = -1;
                    Log.d(TAG, "BottomSheet Dragging.");
                } else if (newState == BottomSheetBehavior.STATE_SETTLING) {
                    bottomSheetState = -1;
                    Log.d(TAG, "BottomSheet settling.");
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Refresh items
                //mEndlessScrollListener.setCurrentPage(1);
                popularMovies = new APICall(BASE_URL, endpoint, posterW, null, true, activity);
            }
        });
    }

    public static void updateGrid(boolean clear) {
        if (clear)
            data.clear();
        if (popularMovies._results != null)
        {
            for (int i = 0; i < popularMovies._results.size(); i++) {
                data.add(new GridDataModel(
                        popularMovies._results.get(i).getTitle(),
                        popularMovies._results.get(i).getFinalPosterURL(),
                        popularMovies._results.get(i).getFinalBackdropURL(),
                        popularMovies._results.get(i).getReleaseDateString(),
                        popularMovies._results.get(i).getId()));
            }
            adapter.notifyDataSetChanged();
        }
        swipeRefreshLayout.setRefreshing(false);
    }

    public void getSpanAndPosterW() {
        if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                == Configuration.SCREENLAYOUT_SIZE_LARGE) {
            spanCount = 5;
            posterW = 342;
        } else {
            spanCount = 3;
            posterW = 185;
        }

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int density = metrics.densityDpi;

        if (posterW == 185 && density > 300) {
            Log.i(TAG, "high density screen = " +
                    density +
                    ". Setting grid poster size to w342.");
            posterW = 342;
        }

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

            if (spanCount == 5)
                spanCount = 7;

            else if (spanCount == 3 && density >= 240)
                spanCount = 5;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (bottomSheetState == 1) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_popular) {
            endpoint = "popular";
            activity_title = "Popular";
        } else if (id == R.id.nav_top_rated) {
            endpoint = "top_rated";
            activity_title = "Top Rated";
        } else if (id == R.id.nav_now_playing) {
            endpoint = "now_playing";
            activity_title = "In Theatres Now";
        } else if (id == R.id.nav_upcoming) {
            endpoint = "upcoming";
            activity_title = "Upcoming";
        }

        if (id != currentNavSelection) {
            currentNavSelection = id;
            popularMovies = new APICall(BASE_URL, endpoint, posterW, null, true, this);
            setTitle(activity_title);
        }

        if (bottomSheetState == 1)
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
