package com.example.distributed_front_end;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import common.Request;
import common.Response;
import common.RequestType;

public class MainActivity extends AppCompatActivity {

    TextView resultText;
    Button connectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultText = findViewById(R.id.resultText);
        connectButton = findViewById(R.id.connectButton);

        connectButton.setOnClickListener(v -> {

            new Thread(() -> {
                try {
                    Socket socket = new Socket("10.0.2.2", 5000);

                    ObjectOutputStream out =
                            new ObjectOutputStream(socket.getOutputStream());
                    out.flush();

                    ObjectInputStream in =
                            new ObjectInputStream(socket.getInputStream());

                    Request request = Request.searchGames("LuckyWheel", "provider1", null, null, null);

                    out.writeObject(request);
                    out.flush();

                    Response response = (Response) in.readObject();

                    runOnUiThread(() ->
                            resultText.setText(response.toString())
                    );

                    socket.close();

                } catch (Exception e) {
                    runOnUiThread(() ->
                            resultText.setText("Error: " + e.getMessage())
                    );
                }
            }).start();

        });
    }
}