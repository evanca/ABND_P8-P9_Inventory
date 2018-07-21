package com.example.ivanna.inventory;

import android.animation.ObjectAnimator;
import android.app.LoaderManager;
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ivanna.inventory.ProductContract.ProductEntry;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int PRODUCT_LOADER = 0;
    ProductCursorAdapter mAdapter;
    private TextView mSearchTextView;
    private ListView warehouseItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        // Check if we have a saved pin code, otherwise prompt to set a pin code:
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String pinCode = sharedPref.getString(getString(R.string.pin_secret_key), null);

        if (pinCode == null) {
            showPinDialog();
        }

        mSearchTextView = findViewById(R.id.search_instructions);
        mSearchTextView.setVisibility(View.GONE);

        // Find ListView to populate
        warehouseItems = findViewById(R.id.warehouse_listview);

        // Set up bottom navigation icons to switch between top-level content views with a single tap:
        final BottomNavigationView mBottomNav = findViewById(R.id.bottom_navigation);
        mBottomNav.setSelectedItemId(R.id.main_nav);
        BottomNavigationViewHelper.disableShiftMode(mBottomNav);
        mBottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.editor_nav:
                        Intent open_editor = new Intent(MainActivity.this, EditorActivity.class);
                        startActivity(open_editor);
                        break;
                    case R.id.search_nav:
                        // Show a "Search" action on the same screen
                        getSupportActionBar().show();
                        mSearchTextView.setVisibility(View.VISIBLE);
                        mSearchTextView.setText(R.string.search_instructions);
                        setTitle(getString(R.string.search));
                        // Remove an empty view if no products were found
                        View emptyView = findViewById(R.id.invisible_view);
                        warehouseItems.setEmptyView(emptyView);
                        break;
                    case R.id.main_nav:
                        // This activity may show search results instead of all products, so we need
                        // to refresh it if user taps a "Home" button
                        getSupportActionBar().hide();
                        mSearchTextView.setVisibility(View.GONE);
                        mAdapter = new ProductCursorAdapter(getApplicationContext(), null);
                        warehouseItems.setAdapter(mAdapter);
                        prepareLoader();
                        break;
                    case R.id.stats_nav:
                        Intent open_stats = new Intent(MainActivity.this, StatsActivity.class);
                        startActivity(open_stats);
                        break;
                    case R.id.settings_nav:
                        Intent open_settings = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(open_settings);
                        break;
                }
                return true;
            }
        });

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items
        View emptyView = findViewById(R.id.empty_view);
        warehouseItems.setEmptyView(emptyView);
        // Add animation to an empty view:
        if (emptyView.getVisibility() == View.VISIBLE) {
            ImageView i = findViewById(R.id.empty_view_image);
            ObjectAnimator animation = ObjectAnimator.ofFloat(i, "translationX", 300f, -200f, 0f);
            animation.setDuration(3500);
            animation.start();
        }

        // Setup cursor adapter
        mAdapter = new ProductCursorAdapter(this, null);

        // Attach cursor adapter to the ListView
        warehouseItems.setAdapter(mAdapter);

        // Prepare the loader
        // Saved to a private method for being able to be called from OnNavigationItemSelectedListener
        prepareLoader();

        warehouseItems.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                Uri currentUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id);
                intent.setData(currentUri);
                startActivity(intent);
            }
        });

        // Handle a search intent
        handleIntent(getIntent());
    }

    private void prepareLoader() {
        getLoaderManager().initLoader(PRODUCT_LOADER, null, this);
    }

    // This is called when a new Loader needs to be created.
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.

        // Define a projection that specifies which columns from the database
        // you will actually use after this query
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_MODEL,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_SHELF,
                ProductEntry.COLUMN_PRODUCT_SUPPLIER,
                ProductEntry.COLUMN_PRODUCT_DATESTAMP};

        return new CursorLoader(
                this,
                ProductEntry.CONTENT_URI,
                projection,
                null,
                null,
                null);
    }

    // This method is guaranteed to be called prior to the release of the last data that was
    // supplied for this loader.
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
    }

    // This method is called when a previously created loader is being reset, thus making its data
    // unavailable.
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    /**
     * Implementing search interface START
     */
    @Override
    protected void onNewIntent(Intent intent) {
        // Because this activity has set launchMode="singleTop", the system calls this method
        // to deliver the intent if this activity is currently the foreground activity when
        // invoked again (when the user executes a search from this activity, we don't create
        // a new instance of this activity, so the system delivers the search intent here)
        handleIntent(intent);

    }

    private void handleIntent(Intent intent) {
        // TODO: Implement recent query suggestions?
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            // Handles a click on a search suggestion & launches editor activity
            Intent i = new Intent(this, EditorActivity.class);
            i.setData(intent.getData());
            startActivity(i);
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            // Handles a search query
            String searchQuery = intent.getStringExtra(SearchManager.QUERY);
            showResults(searchQuery);
        }
    }

    // Searches the warehouse and displays results for the given query
    private void showResults(String searchQuery) {
        Cursor searchCursor = managedQuery(
                ProductEntry.CONTENT_URI,
                new String[]{ProductEntry._ID,
                        ProductEntry.COLUMN_PRODUCT_NAME,
                        ProductEntry.COLUMN_PRODUCT_MODEL,
                        ProductEntry.COLUMN_PRODUCT_PRICE,
                        ProductEntry.COLUMN_PRODUCT_QUANTITY,
                        ProductEntry.COLUMN_PRODUCT_SHELF,
                        ProductEntry.COLUMN_PRODUCT_SUPPLIER,
                        ProductEntry.COLUMN_PRODUCT_DATESTAMP},

                // Search by product name or product model
                ProductEntry.COLUMN_PRODUCT_NAME + " LIKE ?" +
                        " OR " + ProductEntry.COLUMN_PRODUCT_MODEL + " LIKE ?",
                new String[]{searchQuery, searchQuery},

                null);

        if (searchCursor == null) {
            // There are no results
            mSearchTextView.setText(getString(R.string.no_results, searchQuery));

        } else {
            // Display the number of results
            int count = searchCursor.getCount();
            String countString = getResources().getQuantityString(R.plurals.search_results,
                    count, count, searchQuery);
            mSearchTextView.setText(countString);

            // Setup search cursor adapter
            mAdapter = new ProductCursorAdapter(this, searchCursor);

            // Attach search cursor adapter to the ListView
            warehouseItems.setAdapter(mAdapter);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                onSearchRequested();
                return true;
            default:
                return false;
        }
    }
    // Implementing search interface END

    // Pin code dialog starts here:
    public void showPinDialog() {

        LayoutInflater li = LayoutInflater.from(this);
        View dialogView = li.inflate(R.layout.dialog_pin_code, null);
        AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(this);
        mDialogBuilder.setView(dialogView);

        final EditText pinInput = dialogView.findViewById(R.id.pin_edit_text);
        final EditText pinValidate = dialogView.findViewById(R.id.pin_validate);
        final TextView t = dialogView.findViewById(R.id.pin_message);

        pinValidate.setVisibility(View.GONE);

        mDialogBuilder.setCancelable(false);
        mDialogBuilder.setPositiveButton(R.string.save,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Do nothing here because we override this button later
                    }
                });
        final AlertDialog dialog = mDialogBuilder.create();
        dialog.show();
        //Overriding the handler immediately after show:
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get pin value
                String pin = pinInput.getText().toString();
                pinInput.setVisibility(View.GONE);
                if (pin.isEmpty() || pin.length() < 4) {
                    pinInput.setError(getString(R.string.pin_input_error));
                } else {
                    // Continue with pin re-enter validation
                    pinValidate.setVisibility(View.VISIBLE);
                    t.setGravity(Gravity.CENTER);
                    t.setText(getString(R.string.pin_re_enter));

                    if (!t.getText().toString().isEmpty()) {
                        String pinValidation = pinValidate.getText().toString();
                        if (!pinValidation.isEmpty() && pinValidation.length() >= 4) {
                            // Check if user entered the same pin twice:
                            if (pin.equals(pinValidation)) {
                                // User input was the same both times, so continue with saving the pin
                                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putString(getString(R.string.pin_secret_key), pin);
                                editor.apply();
                                Toast.makeText(MainActivity.this, getString(R.string.pin_saved),
                                        Toast.LENGTH_LONG).show();
                                // Finally, close the dialog
                                dialog.dismiss();
                            } else {
                                // Pins do not match, so show an error and start over
                                Toast.makeText(MainActivity.this, getString(R.string.pin_no_match),
                                        Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                                showPinDialog();
                            }
                        }
                    }
                }
            }
        });
    }
}
