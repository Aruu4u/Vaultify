package com.example.vaultify.Login_signup;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.vaultify.MainActivity;
import com.example.vaultify.R;

import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    EditText email, password;
    Button loginBtn, signupBtn;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginBtn);
        signupBtn = findViewById(R.id.signupBtn);


        loginBtn.setOnClickListener(v -> loginUser());

        signupBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
        });
    }

    private void loginUser() {
        String userEmail = email.getText().toString().trim();
        String userPass = password.getText().toString().trim();

        if (userEmail.isEmpty() || userPass.isEmpty()) {
            Toast.makeText(this, "Email & Password required", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();

        try {
            JSONObject json = new JSONObject();
            json.put("AuthFlow", "USER_PASSWORD_AUTH");
            json.put("ClientId", CognitoConfig.CLIENT_ID);

            JSONObject authParams = new JSONObject();
            authParams.put("USERNAME", userEmail);
            authParams.put("PASSWORD", userPass);

            json.put("AuthParameters", authParams);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/x-amz-json-1.1")
            );

            Request request = new Request.Builder()
                    .url("https://cognito-idp." + CognitoConfig.REGION + ".amazonaws.com/")
                    .addHeader("X-Amz-Target", "AWSCognitoIdentityProviderService.InitiateAuth")
                    .addHeader("Content-Type", "application/x-amz-json-1.1")
                    .post(body)
                    .build();

            new Thread(() -> {
                try (Response response = client.newCall(request).execute()) {

                    String responseBody = response.body() != null
                            ? response.body().string()
                            : "";

                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
//                            Toast.makeText(LoginActivity.this,
//                                    "Login Success",
//                                    Toast.LENGTH_LONG).show();
//
//                            startActivity(new Intent(this, MainActivity.class));
//
//                            Log.d("LOGIN_RESPONSE", responseBody);
                            try {
                                JSONObject jsonResponse = new JSONObject(responseBody);
                                JSONObject authResult = jsonResponse.getJSONObject("AuthenticationResult");

                                String idToken = authResult.getString("IdToken");

                                // Extract email from token
                                String emailFromToken = extractEmailFromToken(idToken);

                                // Save session
                                getSharedPreferences("app", MODE_PRIVATE)
                                        .edit()
                                        .putString("token", idToken)
                                        .putString("email", emailFromToken)
                                        .apply();
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }

                            Toast.makeText(LoginActivity.this,
                                    "Login Success",
                                    Toast.LENGTH_LONG).show();

                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish(); // important

                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "Account Does not exist,Sign up to continue ",
                                    Toast.LENGTH_LONG).show();
                        }
                    });

                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(LoginActivity.this,
                                    e.getMessage(),
                                    Toast.LENGTH_LONG).show()
                    );
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private String extractEmailFromToken(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            String payload = new String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE));
            JSONObject json = new JSONObject(payload);
            return json.optString("email", "User");
        } catch (Exception e) {
            return "User";
        }
    }
}
