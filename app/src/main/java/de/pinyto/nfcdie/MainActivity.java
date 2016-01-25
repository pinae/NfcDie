package de.pinyto.nfcdie;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private TextView tagIdView;
    private NfcAdapter nfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tagIdView = (TextView) findViewById(R.id.textView_tag);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (!nfcAdapter.isEnabled()) {
            tagIdView.setText(R.string.nfc_disabled);
        }

        handleIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupForegroundDispatch(this, nfcAdapter);
    }

    @Override
    protected void onPause() {
        stopForegroundDispatch(this, nfcAdapter);
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            byte[] id = tag.getId();
            writeRandomBytes(Arrays.copyOfRange(id, 1, id.length));

            tagIdView.setText(Hextools.bytesToHex(Arrays.copyOfRange(id, 1, id.length)));
            int randomNumber = ((id[1] + 128) << 16) + ((id[2] + 128) << 8) + (id[3] + 128);
            ImageView die = (ImageView) findViewById(R.id.imageViewDie);
            switch (randomNumber % 6) {
                case 0:
                    die.setImageResource(R.drawable.d1);
                    break;
                case 1:
                    die.setImageResource(R.drawable.d2);
                    break;
                case 2:
                    die.setImageResource(R.drawable.d3);
                    break;
                case 3:
                    die.setImageResource(R.drawable.d4);
                    break;
                case 4:
                    die.setImageResource(R.drawable.d5);
                    break;
                case 5:
                    die.setImageResource(R.drawable.d6);
                    break;
                default:
                    die.setImageResource(0);
                    break;
            }
        }
    }

    private void writeRandomBytes(byte[] data) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File file = new File(this.getBaseContext().getExternalFilesDir(null), "");
            if (!file.mkdirs() && !file.isDirectory()) {
                Log.e("directory error", "Could not create the directory.");
            }
            file = new File(this.getBaseContext().getExternalFilesDir(null), "NfcRandom");
            if (!file.exists()) {
                try {
                    if (!file.createNewFile()) {
                        Log.e("write error", "Could not write to sd card.");
                    }
                } catch (IOException writeError) {
                    writeError.printStackTrace();
                }
            }
            try {
                FileOutputStream f = new FileOutputStream(file, true);
                try {
                    f.write(data);
                } catch (IOException writeError) {
                    writeError.printStackTrace();
                } finally {
                    f.close();
                }
            } catch (IOException fileError) {
                fileError.printStackTrace();
            }
        }
    }

    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(
                activity.getApplicationContext(), 0, intent, 0);

        String[][] techList = new String[][]{};
        IntentFilter filter = new IntentFilter();
        filter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        IntentFilter[] filters = {filter};

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }
}
