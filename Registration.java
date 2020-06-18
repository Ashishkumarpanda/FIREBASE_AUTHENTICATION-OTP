package com.example.otp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rilixtech.widget.countrycodepicker.CountryCodePicker;

import java.util.concurrent.TimeUnit;

public class Registration extends AppCompatActivity {
    public static final String TAG="TAG";
    FirebaseAuth fAuth;
    EditText phoneNumber,codeEnter;
    FirebaseFirestore fstore;
    Button nxtBtn;
    ProgressBar progressBar;
    TextView state;
    CountryCodePicker codePicker;
    String verificationId;
    PhoneAuthProvider.ForceResendingToken token;
    Boolean verificationInProgress=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        fAuth=FirebaseAuth.getInstance();
        fstore=FirebaseFirestore.getInstance();
        phoneNumber=findViewById(R.id.phone);
        codeEnter=findViewById(R.id.codeEnter);
        nxtBtn=findViewById(R.id.nextBtn);
        progressBar=findViewById(R.id.progressBar);
        state=findViewById(R.id.state);
        codePicker=findViewById(R.id.ccp);

        nxtBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (verificationInProgress) {
                    if (!phoneNumber.getText().toString().isEmpty() && phoneNumber.getText().toString().length() == 10) {
                        String phoneNum = " + " + codePicker.getSelectedCountryCode() + phoneNumber.getText().toString();
                        Log.d("TAG", "onclick:Phone No->" + phoneNum);
                        progressBar.setVisibility(View.VISIBLE);
                        state.setText("Sending OTP");
                        state.setVisibility(View.VISIBLE);
                        requestOtp(phoneNum);


                    } else {
                        phoneNumber.setError("invalid phone number");
                    }
                }else {
                    String userOTP=codeEnter.getText().toString();
                    if (!(userOTP.isEmpty()) && userOTP.length()==6){
                        PhoneAuthCredential credential=PhoneAuthProvider.getCredential(verificationId,userOTP);
                        verifyAuth(credential);

                    }else {
                        codeEnter.setError("valid otp required.");

                    }



                }

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (fAuth.getCurrentUser() !=null)
        {
            progressBar.setVisibility(View.VISIBLE);
            state.setText("Checking");
            state.setVisibility(View.VISIBLE);
            checkUserProfile();
        }
    }

    private void checkUserProfile() {
        DocumentReference docRef=fstore.collection("users").document(fAuth.getCurrentUser().getUid());
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()){
                    startActivity(new Intent(getApplicationContext(),MainActivity.class));
                    finish();
                }
                else {
                    startActivity(new Intent(getApplicationContext(),AddDetails.class));
                    finish();
                }
            }



        });
    }

    private void verifyAuth(PhoneAuthCredential credential) {
        fAuth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful())
                {
                    checkUserProfile();

                }
                else {
                    Toast.makeText(Registration.this,"Authentication failed",Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    private void requestOtp(String phoneNum)
    {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneNum, 60L, TimeUnit.SECONDS, this, new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onCodeAutoRetrievalTimeOut(@NonNull String s) {
                super.onCodeAutoRetrievalTimeOut(s);
                Toast.makeText(Registration.this,"Otp expired,request otp",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);
                progressBar.setVisibility(View.GONE);
                state.setVisibility(View.GONE);
                codeEnter.setVisibility(View.VISIBLE);
                verificationId=s;
                token=forceResendingToken;
                nxtBtn.setText("Verify");
                verificationInProgress=true;
            }

            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                verifyAuth(phoneAuthCredential);

            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Toast.makeText(Registration.this,"verification failed"+e.getMessage(),Toast.LENGTH_SHORT).show();

            }
        });

    }
}
