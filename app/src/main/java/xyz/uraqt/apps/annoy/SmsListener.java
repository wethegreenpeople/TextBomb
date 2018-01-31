package xyz.uraqt.apps.annoy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.widget.Toast;


public class SmsListener extends BroadcastReceiver {
    static String messageBody = "";

    @Override
    public void onReceive(Context context, Intent intent) {
        String bombDefuse = MainActivity.bombDefuse;
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                messageBody = smsMessage.getMessageBody();
            }
        }
    }
}
