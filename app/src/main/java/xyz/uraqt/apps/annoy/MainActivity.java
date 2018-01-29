package xyz.uraqt.apps.annoy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import static android.R.attr.value;
import static android.R.attr.y;
import static android.R.id.edit;
import static xyz.uraqt.apps.annoy.R.id.editTextPhoneNumber;
import static xyz.uraqt.apps.annoy.R.id.seekBarMessageDelay;
import static xyz.uraqt.apps.annoy.R.id.textViewDelayLength;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CheckSMSPermissions();
        UpdateDelayBar();
    }

    // Sending our textbomb here
    public void SendMessages(View view) {
        final String phoneNumber = ((EditText) findViewById(R.id.editTextPhoneNumber)).getText().toString();
        int bombAmount = Integer.parseInt(((EditText) findViewById(R.id.editTextAmountOfTexts)).getText().toString());
        final String messageToSend = ((EditText) findViewById(R.id.editTextMessageToSend)).getText().toString();
        final Handler handler = new Handler(); // setting up a handler for a delay
        final SmsManager smsManager = SmsManager.getDefault();
        final int delayAmount = ((SeekBar)findViewById(R.id.seekBarMessageDelay)).getProgress();

        for(int i = 0; i < bombAmount; ++i)
        {
            handler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    smsManager.sendTextMessage(phoneNumber, "ME", messageToSend, null, null);
                }
            }, ((delayAmount + i) * 1000));
        }

        Toast.makeText(getApplicationContext(), "Text bomb sent", Toast.LENGTH_SHORT).show();
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
}
