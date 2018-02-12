package xyz.uraqt.apps.textbomb;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.EntypoModule;
import com.joanzapata.iconify.fonts.FontAwesomeModule;
import com.joanzapata.iconify.fonts.IoniconsModule;
import com.joanzapata.iconify.fonts.MaterialCommunityModule;
import com.joanzapata.iconify.fonts.MaterialModule;
import com.joanzapata.iconify.fonts.MeteoconsModule;
import com.joanzapata.iconify.fonts.SimpleLineIconsModule;
import com.joanzapata.iconify.fonts.TypiconsModule;
import com.joanzapata.iconify.fonts.WeathericonsModule;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.R.attr.button;
import static xyz.uraqt.apps.textbomb.MainActivity.handler;
import static xyz.uraqt.apps.textbomb.R.string.messageToSend;

public class MainActivity extends AppCompatActivity {
    static String bombDefuse = null;
    static Handler handler = new Handler(); // setting up a handler for a delay
    public final int PICK_CONTACT = 2015;

    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CheckSMSPermissions();
        UpdateDelayBar();
        MonitorSpinner();
        MonitorTextView();
        ContactPicker();
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        MobileAds.initialize(this, "ca-app-pub-1592176704950004~9006530595");
    }

    public void PressSend(View view)
    {
        try {
            InitListener(); // Listener for our defusal message
            String typeOfMessage = ((Spinner) findViewById(R.id.spinnerMessageToSend)).getSelectedItem().toString();
            String animal = null;

            String phoneNumber = ((EditText) findViewById(R.id.editTextPhoneNumber)).getText().toString();
            int bombAmount = Integer.parseInt(((EditText) findViewById(R.id.editTextAmountOfTexts)).getText().toString());
            int delayAmount = 0;
            // Getting the delay amount from either the seeker or edittext depending on whats active
            if ((findViewById(R.id.seekBarMessageDelay)).getVisibility() == View.VISIBLE)
            {
                delayAmount = ((SeekBar) findViewById(R.id.seekBarMessageDelay)).getProgress();
            }
            else if ((findViewById(R.id.seekBarMessageDelay)).getVisibility() != View.VISIBLE)
            {
                delayAmount =  Integer.parseInt(((EditText) findViewById(R.id.editTextMessageDelay)).getText().toString());
            }

            String messageToSend = ((EditText) findViewById(R.id.editTextMessageToSend)).getText().toString();
            bombDefuse = ((EditText) findViewById(R.id.editTextStopMessage)).getText().toString();

            // Are we sending a custom message?
            if (typeOfMessage.equals("Custom"))
            {
                if (CheckLimits(phoneNumber, bombAmount, messageToSend))
                {
                    SendCustomMessage(phoneNumber, bombAmount, messageToSend, delayAmount, bombDefuse);
                    SwapSendButton(1);
                    Toast.makeText(getApplicationContext(), "Text bomb sent", Toast.LENGTH_SHORT).show();

                }
            }
            // We're sending an animal fact
            else if (!typeOfMessage.equals("Custom"))
            {
                // We're taking the first word from our spinner and that's our animal that we're requesting
                // facts for
                animal = typeOfMessage.split(" ")[0].toString().toLowerCase();
                if (CheckLimits(phoneNumber, bombAmount))
                {
                    GetAnimalFact animalFact = new GetAnimalFact(getApplicationContext(), phoneNumber, bombAmount, delayAmount, bombDefuse, animal, this);
                    animalFact.execute();
                    SwapSendButton(1);
                }
            }
        }
        catch (NumberFormatException e)
        {
            Toast.makeText(getApplicationContext(), "Field(s) Missing", Toast.LENGTH_LONG).show();
        }
        catch (Exception e)
        {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
        }

    }

    // Cancels our textbomb
    public void PressStop(View view)
    {
        this.handler.removeCallbacksAndMessages(null);
        SwapSendButton(0);
    }

    // Limits for this version of the app
    // This method does not verify message length
    public Boolean CheckLimits(String phoneNumber, int bombAmount)
    {
        Boolean passed = true;
        if (phoneNumber.length() < 9 || phoneNumber.length() > 11)
        {
            passed = false;
            Toast.makeText(getApplicationContext(), "Invalid phone number. Re-enter valid 10 digit phone number", Toast.LENGTH_LONG).show();
            return passed;
        }
        if (bombAmount > 100)
        {
            passed = false;
            Toast.makeText(getApplicationContext(), "Cannot send more than 100 texts at a time", Toast.LENGTH_LONG).show();
            return passed;
        }

        return passed;
    }

    // Limits for this version of the app
    // This method verifies message length
    public Boolean CheckLimits(String phoneNumber, int bombAmount, String messageToSend)
    {
        Boolean passed = true;
        if (phoneNumber.length() < 9 || phoneNumber.length() > 11)
        {
            passed = false;
            Toast.makeText(getApplicationContext(), "Invalid phone number. Re-enter valid 10 digit phone number", Toast.LENGTH_LONG).show();
            return passed;
        }
        if (bombAmount > 100)
        {
            passed = false;
            Toast.makeText(getApplicationContext(), "Cannot send more than 100 texts at a time", Toast.LENGTH_LONG).show();
            return passed;
        }
        if (messageToSend.length() > 140)
        {
            passed = false;
            Toast.makeText(getApplicationContext(), "Bomb message cannot be greater than 140 characters", Toast.LENGTH_LONG).show();
            return passed;
        }

        return passed;
    }

    // Swapping our send button with our stop button
    // 0 = send
    // 1 = stop
    public void SwapSendButton(int swap)
    {
        if (swap == 0)
        {
            findViewById(R.id.imageButton2).setVisibility(View.VISIBLE);
            findViewById(R.id.imageButton3).setVisibility(View.GONE);
        }
        else if (swap == 1)
        {
            findViewById(R.id.imageButton2).setVisibility(View.GONE);
            findViewById(R.id.imageButton3).setVisibility(View.VISIBLE);
        }
    }

    // Sending our textbomb here
    public void SendCustomMessage(final String phoneNumber, final int bombAmount, final String messageToSend, final int delayAmount, final String bombDefuse) {
        final SmsManager smsManager = SmsManager.getDefault();

        final Runnable runnable = new Runnable() {
            int count =  0; // count of how many times we've sent a message
            public void run() {
                // If we've sent all the messages
                if (count >= bombAmount)
                {
                    SwapSendButton(0);
                }
                // If we get our defuseal text
                if (bombDefuse.equals(SmsListener.messageBody))
                {
                    Toast.makeText(getApplicationContext(), "Bomb Defused", Toast.LENGTH_SHORT).show();
                }
                else if (count < bombAmount && !bombDefuse.equals(SmsListener.messageBody))
                {
                    // a delay of 0 doesn't work IRL
                    // So in truth the quickest you can send a text bomb is 1 second apart
                    if (delayAmount < 1)
                    {
                        smsManager.sendTextMessage(phoneNumber, "ME", messageToSend, null, null);
                        handler.postDelayed(this, ((delayAmount + 1) * 1000));
                        ++count;
                    }
                    else
                    {
                        smsManager.sendTextMessage(phoneNumber, "ME", messageToSend, null, null);
                        handler.postDelayed(this, (delayAmount * 1000));
                        ++count;
                    }
                }
            }
        };
        handler.post(runnable);
    }

    public void InitListener()
    {
        // Setting up the listener for incoming text messages; for the bomb defuser.
        Intent listener = new Intent(this, SmsListener.class);
        listener.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startService(listener);
    }

    public boolean CheckSMSPermissions() {
        int hasSendSMSPermission = 0;
        boolean activePermissions = false; // this is the variable we're returning
        // Check if we're Marshmellow or  over
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasSendSMSPermission = checkSelfPermission(Manifest.permission.SEND_SMS);
            // If we don't have permissions request them
            if (hasSendSMSPermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 130);
            } else if (hasSendSMSPermission == PackageManager.PERMISSION_GRANTED) {
                return activePermissions;
            }
        }
        return activePermissions;
    }

    public void UpdateDelayBar()
    {
        SeekBar seekbar = (SeekBar) findViewById(R.id.seekBarMessageDelay);
        final TextView textViewDelay = (TextView) findViewById(R.id.textViewDelayLength);

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            //link "seekbar" with seek bar change listener
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textViewDelay.setText("Delay: " + progress + " seconds"); //update "progress" value and pass it to textview
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });
    }

    // Checking the value in our spinner
    private void MonitorSpinner()
    {
        Spinner spinner = (Spinner) findViewById(R.id.spinnerMessageToSend);
        final EditText message = (EditText) findViewById(R.id.editTextMessageToSend);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String item = adapterView.getItemAtPosition(i).toString();
                // If we're not sending a custom message, we're not allowing the user
                // to type anything into the editTextBox for custom message.
                if (!item.equals("Custom"))
                {
                    message.setEnabled(false);
                }
                else if (item.equals("Custom"))
                {
                    message.setEnabled(true);
                }
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                return;
            }
        });
    }

    // If textview is clicked, we'll add an edit text box for typing a number in
    public void MonitorTextView()
    {
        TextView textview = (TextView) findViewById(R.id.textViewDelayLength);
        textview.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                EditText editText = (EditText) findViewById(R.id.editTextMessageDelay);
                SeekBar seek = (SeekBar) findViewById(R.id.seekBarMessageDelay);
                if (editText.getVisibility() == View.GONE)
                {
                    editText.setVisibility(View.VISIBLE);
                    seek.setVisibility(View.GONE);
                }
                else if (editText.getVisibility() == View.VISIBLE)
                {
                    editText.setVisibility(View.GONE);
                    seek.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    public void ContactPicker()
    {
        (findViewById(R.id.contactButton)).setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(i, PICK_CONTACT);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_CONTACT && resultCode == RESULT_OK) {
            Uri contactUri = data.getData();
            Cursor cursor = getContentResolver().query(contactUri, null, null, null, null);
            cursor.moveToFirst();
            int column = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            String phoneNumber = cursor.getString(column);
            phoneNumber = CleanPhoneNumber(phoneNumber);
            ((EditText) findViewById(R.id.editTextPhoneNumber)).setText(phoneNumber);
        }
    }

    public String CleanPhoneNumber(String phoneNumber)
    {
        phoneNumber = phoneNumber.replaceAll("[^0-9]+", "");
        return phoneNumber;
    }
}

// Grabbing our animal fact
class GetAnimalFact extends AsyncTask<Void, Void, String[]> {
    private Context ccontext;

    String phoneNumber, bombDefuse, animal = null;
    int bombAmount, delayAmount;
    public MainActivity main;

    public GetAnimalFact(Context context, String phoneNumber, int bombAmount, int delayAmount, String bombDefuse, String animal, MainActivity main)
    {
        this.phoneNumber = phoneNumber;
        this.bombAmount = bombAmount;
        this.delayAmount = delayAmount;
        this.bombDefuse = bombDefuse;
        this.animal = animal;
        this.main = main;
        ccontext = context;
    }

    @Override
    protected String[] doInBackground(Void ... params) {
        OkHttpClient httpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://uraqt.xyz/api/animalfacts/animal/" + animal)
                .build();

        Response response = null;
        // We're grabbing all the facts we need at one time, storing them into an array
        // and then reading our facts out of the array when we send the text bomb
        String[] facts = new String[bombAmount];
        try {
            for (int i = 0; i < bombAmount; ++i)
            {
                response = httpClient.newCall(request).execute();
                // If we come back and change this and verify the length of the fact here
                // we can grab a new fact if the length is too long to send via sms
                // as of right now we're just not sending facts that are longer than 140 chars
                facts[i] = response.body().string();
                Log.i("textbomb", facts.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return facts;
    }

    @Override
    protected void onPostExecute(String[] facts)
    {
        SendAnimalFact(facts, phoneNumber, bombAmount, delayAmount, main);
    }

    private void SendAnimalFact(final String[] messageToSend, final String phoneNumber, final int bombAmount, final int delayAmount, final MainActivity main)
    {
        final SmsManager smsManager = SmsManager.getDefault();

        final Runnable runnable = new Runnable() {
            int count =  0;
            public void run() {
                if (count >= bombAmount)
                {
                    main.SwapSendButton(0);
                }
                if (bombDefuse.equals(SmsListener.messageBody))
                {
                    Toast.makeText(ccontext, "Bomb Defused", Toast.LENGTH_SHORT).show();
                    Log.i("Sending", "Textbomb defused");
                }
                else if (count < bombAmount && !bombDefuse.equals(SmsListener.messageBody))
                {
                    if (main.CheckLimits(phoneNumber, bombAmount, messageToSend[count]))
                    {
                        if (delayAmount < 1)
                        {
                            smsManager.sendTextMessage(phoneNumber, "ME", messageToSend[count], null, null);
                            handler.postDelayed(this, ((delayAmount + 1) * 1000));
                            ++count;
                        }
                        else
                        {
                            smsManager.sendTextMessage(phoneNumber, "ME", messageToSend[count], null, null);
                            handler.postDelayed(this, (delayAmount * 1000));
                            ++count;
                        }
                    }
                    else
                    {
                        handler.postDelayed(this, (delayAmount * 1000));
                        ++count;
                    }
                }
            }
        };
        handler.post(runnable);

        Toast.makeText(ccontext, "Text bomb sent", Toast.LENGTH_SHORT).show();
    }
}
