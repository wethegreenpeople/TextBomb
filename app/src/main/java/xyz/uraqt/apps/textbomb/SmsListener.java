package xyz.uraqt.apps.textbomb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;


public class SmsListener extends BroadcastReceiver {
    static String messageBody = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        String bombDefuse = MainActivity.bombDefuse;
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                messageBody = smsMessage.getMessageBody().toLowerCase();
            }
        }
    }
}
