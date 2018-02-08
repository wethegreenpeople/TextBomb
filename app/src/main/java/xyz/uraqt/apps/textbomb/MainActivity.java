package xyz.uraqt.apps.textbomb;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static xyz.uraqt.apps.textbomb.MainActivity.handler;

public class MainActivity extends AppCompatActivity {
    static String bombDefuse = null;
    static Handler handler = new Handler(); // setting up a handler for a delay

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CheckSMSPermissions();
        UpdateDelayBar();
        MonitorSpinner();
    }

    public void PressSend(View view)
    {
        InitListener();
        String typeOfMessage = ((Spinner) findViewById(R.id.spinnerMessageToSend)).getSelectedItem().toString();
        String animal = null;

        final String phoneNumber = ((EditText) findViewById(R.id.editTextPhoneNumber)).getText().toString();
        final int bombAmount = Integer.parseInt(((EditText) findViewById(R.id.editTextAmountOfTexts)).getText().toString());
        final int delayAmount = ((SeekBar) findViewById(R.id.seekBarMessageDelay)).getProgress();
        final String messageToSend = ((EditText) findViewById(R.id.editTextMessageToSend)).getText().toString();
        bombDefuse = ((EditText) findViewById(R.id.editTextStopMessage)).getText().toString();

        if (typeOfMessage.equals("Custom"))
        {
            if (CheckLimits(phoneNumber, bombAmount, messageToSend))
            {
                SendCustomMessage();
                SwapSendButton(1);
            }
        }
        else if (!typeOfMessage.equals("Custom"))
        {
            animal = typeOfMessage.split(" ")[0].toString().toLowerCase();

            GetAnimalFact animalFact = new GetAnimalFact(getApplicationContext(), phoneNumber, bombAmount, delayAmount, bombDefuse, animal, this);
            animalFact.execute();
            SwapSendButton(1);
        }
    }

    public void PressStop(View view)
    {
        this.handler.removeCallbacksAndMessages(null);
        SwapSendButton(0);
    }

    public Boolean CheckLimits(String phoneNumber, int bombAmount)
    {
        Boolean passed = true;
        if (phoneNumber.length() < 9 || phoneNumber.length() > 11)
        {
            passed = false;
            Toast.makeText(getApplicationContext(), "Invalid phone number. Re-enter valid 10 digit phone number", Toast.LENGTH_LONG).show();
        }
        if (bombAmount > 50)
        {
            passed = false;
            Toast.makeText(getApplicationContext(), "Cannot send more than 50 texts at a time", Toast.LENGTH_LONG).show();
        }

        return passed;
    }

    public Boolean CheckLimits(String phoneNumber, int bombAmount, String messageToSend)
    {
        Boolean passed = true;
        if (phoneNumber.length() < 9 || phoneNumber.length() > 11)
        {
            passed = false;
            Toast.makeText(getApplicationContext(), "Invalid phone number. Re-enter valid 10 digit phone number", Toast.LENGTH_LONG).show();
        }
        if (bombAmount > 50)
        {
            passed = false;
            Toast.makeText(getApplicationContext(), "Cannot send more than 50 texts at a time", Toast.LENGTH_LONG).show();
        }
        if (messageToSend.length() > 140)
        {
            passed = false;
            Toast.makeText(getApplicationContext(), "Bomb message cannot be greater than 140 characters", Toast.LENGTH_LONG).show();
        }

        return passed;
    }

    public void SwapSendButton(int swap)
    {
        if (swap == 0)
        {
            findViewById(R.id.buttonSend).setVisibility(View.VISIBLE);
            findViewById(R.id.buttonStop).setVisibility(View.GONE);
        }
        else if (swap == 1)
        {
            findViewById(R.id.buttonSend).setVisibility(View.GONE);
            findViewById(R.id.buttonStop).setVisibility(View.VISIBLE);
        }
    }

    // Sending our textbomb here
    public void SendCustomMessage() {
        final String phoneNumber = ((EditText) findViewById(R.id.editTextPhoneNumber)).getText().toString();
        final int bombAmount = Integer.parseInt(((EditText) findViewById(R.id.editTextAmountOfTexts)).getText().toString());
        final String messageToSend = ((EditText) findViewById(R.id.editTextMessageToSend)).getText().toString();
        final int delayAmount = ((SeekBar)findViewById(R.id.seekBarMessageDelay)).getProgress();
        bombDefuse = ((EditText) findViewById(R.id.editTextStopMessage)).getText().toString();
        final SmsManager smsManager = SmsManager.getDefault();

        final Runnable runnable = new Runnable() {
            int count =  0;
            public void run() {
                if (count >= bombAmount)
                {
                    SwapSendButton(0);
                }
                if (bombDefuse.equals(SmsListener.messageBody))
                {
                    Toast.makeText(getApplicationContext(), "Bomb Defused", Toast.LENGTH_SHORT).show();
                }
                else if (count < bombAmount && !bombDefuse.equals(SmsListener.messageBody))
                {
                    smsManager.sendTextMessage(phoneNumber, "ME", messageToSend, null, null);
                    handler.postDelayed(this, (delayAmount * 1000));
                    ++count;
                }
            }
        };
        handler.post(runnable);

        Toast.makeText(getApplicationContext(), "Text bomb sent", Toast.LENGTH_SHORT).show();
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

    private void MonitorSpinner()
    {
        Spinner spinner = (Spinner) findViewById(R.id.spinnerMessageToSend);
        final EditText message = (EditText) findViewById(R.id.editTextMessageToSend);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String item = adapterView.getItemAtPosition(i).toString();
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
}

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
        String[] facts = new String[bombAmount];
        try {
            for (int i = 0; i < bombAmount; ++i)
            {
                response = httpClient.newCall(request).execute();
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
                        smsManager.sendTextMessage(phoneNumber, "ME", messageToSend[count], null, null);
                        handler.postDelayed(this, (delayAmount * 1000));
                        ++count;
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
