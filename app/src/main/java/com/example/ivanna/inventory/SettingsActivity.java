package com.example.ivanna.inventory;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle(R.string.action_settings);

    }

    public void checkPinCode(View v) {
        // Get the pin code from SharedPreferences
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final String pinCode = sharedPref.getString(getString(R.string.pin_secret_key), null);

        // Pin validation dialog starts here
        LayoutInflater li = LayoutInflater.from(this);
        View dialogView = li.inflate(R.layout.dialog_enter_pin, null);
        AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(this);
        mDialogBuilder.setView(dialogView);
        final EditText pinEnter = dialogView.findViewById(R.id.pin_enter);
        mDialogBuilder.setCancelable(false);
        mDialogBuilder.setPositiveButton(R.string.OK,
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
                String pin = pinEnter.getText().toString();
                if (pin.isEmpty() || pin.length() < 4) {
                    pinEnter.setError(getString(R.string.pin_input_error));
                } else {
                    // Continue with pin validation
                    if (!pin.equals(pinCode)) {
                        // PIN does not match, so show error and close dialog
                        Toast.makeText(SettingsActivity.this, getString(R.string.pin_incorrect),
                                Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    } else {
                        // PIN is OK, close this dialog and continue with delete method
                        dialog.dismiss();
                        deleteEverything(v);
                    }
                }

            }
        });
    }

    // Method to export the database to CSV. Has to be static to be called from a static PreferenceFragment.
    private static void exportToCsv(Context c) {

        // First check external storage permissions:
        int permission = ActivityCompat.checkSelfPermission(c, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(c, R.string.storage_not_granted, Toast.LENGTH_SHORT).show();
            return;
        }

        ProductDbHelper dbhelper = new ProductDbHelper(c);
        // Return the primary shared/external storage directory.
        File exportDir = new File(Environment.getExternalStorageDirectory(), "");
        if (!exportDir.exists()) {
            // Creates the directory named by this abstract pathname, including any necessary but nonexistent parent directories.
            exportDir.mkdirs();
        }

        File file = new File(exportDir, "inventory_export.csv");
        try {
            file.createNewFile();

            // Using "opencsv" parser library for Java:
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));

            SQLiteDatabase db = dbhelper.getReadableDatabase();
            Cursor curCSV = db.rawQuery("SELECT * FROM products", null);
            csvWrite.writeNext(curCSV.getColumnNames());
            while (curCSV.moveToNext()) {
                // Which columns will be exported
                String arrStr[] = {curCSV.getString(0),
                        curCSV.getString(1),
                        curCSV.getString(2),
                        curCSV.getString(3),
                        curCSV.getString(4),
                        curCSV.getString(5),
                        curCSV.getString(6),
                        curCSV.getString(7),
                        curCSV.getString(8)};
                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            curCSV.close();

            Toast.makeText(c, c.getString(R.string.export_successfull), Toast.LENGTH_LONG).show();

            // After export is succesfull, send the file via e-mail
            Uri uri = Uri.parse("file://" + exportDir + "/" + "inventory_export.csv");

            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());

            Intent emailIntent = new Intent(Intent.ACTION_SEND);

            emailIntent.setType("application/vnd.ms-excel");
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, c.getString(R.string.export_subject));
            emailIntent.putExtra(Intent.EXTRA_TEXT, c.getString(R.string.export_successfull));
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);

            c.startActivity(Intent.createChooser(emailIntent, c.getString(R.string.export_send)));
        } catch (Exception sqlEx) {
            Log.e(c.getClass().getSimpleName(), sqlEx.getMessage(), sqlEx);
            Toast.makeText(c, R.string.export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteEverything(View v) {
        // Next dialog after pin validation starts here
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.delete_all_items_msg));
        builder.setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                // Here we delete ALL the data using ContentResolver().delete
                int rowsDeleted = getContentResolver().delete(ProductContract.ProductEntry.CONTENT_URI, null, null);

                if (rowsDeleted == 0) {
                    Toast.makeText(getApplicationContext(), getString(R.string.delete_all_items_failed),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.delete_all_items_successful),
                            Toast.LENGTH_SHORT).show();
                }

            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        builder.setTitle(getString(R.string.warning));
        builder.setIcon(R.drawable.delete);

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public static class InventoryPreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

        EditTextPreference editTextPreference;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            editTextPreference = (EditTextPreference) findPreference(getString(R.string.settings_currency_key_custom));

            // If pre-defined currency is already selected, enable EditTextPreference:
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String currency = sharedPrefs.getString(
                    getString(R.string.settings_currency_key),
                    getString(R.string.settings_currency_EUR));
            if (currency.equals(getString(R.string.settings_currency_custom_title))) {
                editTextPreference.setEnabled(true);
            }

            final ListPreference listPreference = (ListPreference) findPreference(getString(R.string.settings_currency_key));

            // Custom OnPreferenceChangeListener to check if user wants to use a pre-defined or a custom currency:
            listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object value) {

                    int index = listPreference.findIndexOfValue(value.toString());

                    if (index != -1) {
                        // Check if a "Custom" option is selected
                        if (listPreference.getEntries()[index].equals(getString(R.string.settings_currency_custom_title))) {
                            // User prefers to use a custom currency, so enable EditTextPreference
                            editTextPreference.setEnabled(true);
                        } else {
                            // Pre-defined currency is selected, so disable EditTextPreference
                            editTextPreference.setText("");
                            editTextPreference.setSummary("");
                            editTextPreference.setEnabled(false);
                        }
                    }
                    return true;
                }
            });

            Preference maxQuantityPref = findPreference(getString(R.string.settings_max_quantity_key));
            bindPreferenceSummaryToValue(maxQuantityPref);

            Preference customCurrency = findPreference(getString(R.string.settings_currency_key_custom));
            bindPreferenceSummaryToValue(customCurrency);

            Preference stepPref = findPreference(getString(R.string.settings_step_key));
            bindPreferenceSummaryToValue(stepPref);

            Preference exportPref = findPreference(getString(R.string.settings_export_csv_key));
            exportPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(getString(R.string.export_dialog));
                    builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            exportToCsv(getActivity());
                        }
                    });
                    builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });

                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();

                    return true;
                }
            });

            Preference creditsPref = findPreference(getString(R.string.settings_credits_key));
            creditsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(getString(R.string.credits_summary));
                    builder.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });

                    builder.setTitle(getString(R.string.settings_credits_no_symbol));
                    builder.setIcon(R.drawable.information_outline);

                    // Create and show the AlertDialog
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();

                    return true;
                }
            });
        }

        // Called when a Preference has been changed by the user. This is called before the state of
        // the Preference is about to be updated and before the state is persisted.
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            preference.setSummary(stringValue);
            return true;
        }

        // Binds a preference's summary to its value. More specifically, when the preference's value
        // is changed, its summary (line of text below the preference title) is updated to reflect
        // the value. The summary is also immediately updated upon calling this method. The exact
        // display format is dependent on the type of preference.
        private void bindPreferenceSummaryToValue(Preference preference) {
            preference.setOnPreferenceChangeListener(this);
            SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(preference.getContext());
            String preferenceString = preferences.getString(preference.getKey(), "");
            onPreferenceChange(preference, preferenceString);

        }

    }

}

