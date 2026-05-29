package com.kanakku.app;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.kanakku.app.widget.WidgetUpdateService;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class QuickAddActivity extends AppCompatActivity {

    private EditText etAmount, etType, etDate;
    private Spinner spCategory;
    private Button btnSave, btnCancel;
    private ProgressBar progressBar;
    private TextView tvTitle;

    private String selectedDate = "";
    private boolean isRefund = false;

    private static final String[] CATEGORIES = {
        "Food", "Breakfast", "Lunch", "Snacks", "Grocery",
        "Bill", "Transport", "Family", "Entertainment",
        "Subscription", "Health", "Shopping", "Other", "Refund"
    };

    // ── Shared prefs keys (set by WidgetUpdateService) ──
    public static final String PREFS = "kanakku_widget_prefs";
    public static final String KEY_USER_EMAIL = "user_email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_quick_add);

        isRefund = getIntent().getBooleanExtra("is_refund", false);

        initViews();
        setupDate();
        setupCategory();
        setupButtons();

        if (isRefund) {
            tvTitle.setText("↩ Add Refund");
            btnSave.setBackgroundColor(0xFF10B981);
            btnSave.setText("Save Refund");
        }
    }

    private void initViews() {
        tvTitle    = findViewById(R.id.tvTitle);
        etAmount   = findViewById(R.id.etAmount);
        etType     = findViewById(R.id.etType);
        etDate     = findViewById(R.id.etDate);
        spCategory = findViewById(R.id.spCategory);
        btnSave    = findViewById(R.id.btnSave);
        btnCancel  = findViewById(R.id.btnCancel);
        progressBar= findViewById(R.id.progressBar);
    }

    private void setupDate() {
        Calendar cal = Calendar.getInstance();
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);
        selectedDate = String.format("%04d-%02d-%02d", y, m+1, d);
        etDate.setText(String.format("%02d/%02d/%04d", d, m+1, y));

        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                selectedDate = String.format("%04d-%02d-%02d", year, month+1, day);
                etDate.setText(String.format("%02d/%02d/%04d", day, month+1, year));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void setupCategory() {
        String[] catWithEmoji = {
            "🍛 Food", "🍳 Breakfast", "🥗 Lunch", "🍿 Snacks", "🛒 Grocery",
            "⚡ Bill", "🚗 Transport", "👨‍👩‍👧 Family", "🎬 Entertainment",
            "📱 Subscription", "💊 Health", "🛍️ Shopping", "📌 Other", "↩️ Refund"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, catWithEmoji);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(adapter);
        if (isRefund) spCategory.setSelection(catWithEmoji.length - 1);
    }

    private void setupButtons() {
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveExpense());
    }

    private void saveExpense() {
        String amtStr = etAmount.getText().toString().trim();
        String type   = etType.getText().toString().trim();

        if (TextUtils.isEmpty(amtStr)) {
            etAmount.setError("Enter amount");
            etAmount.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(type)) {
            etType.setError("Enter description");
            etType.requestFocus();
            return;
        }

        double amount;
        try { amount = Double.parseDouble(amtStr); }
        catch (NumberFormatException e) { etAmount.setError("Invalid amount"); return; }

        String cat = CATEGORIES[spCategory.getSelectedItemPosition()];
        boolean refund = isRefund || cat.equals("Refund");
        double finalAmount = refund ? -Math.abs(amount) : amount;

        // Get user email from prefs (saved by WidgetUpdateService after login)
        String userEmail = getSharedPreferences(PREFS, MODE_PRIVATE)
            .getString(KEY_USER_EMAIL, "");
        if (TextUtils.isEmpty(userEmail)) {
            Toast.makeText(this, "Open Kanakku app first to sign in", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        btnSave.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        String apiUrl = MainActivity.APP_URL.replace("kanakku.html", "")
            .replace("index.html", "");

        // Build Apps Script URL
        String scriptUrl = "https://script.google.com/macros/s/AKfycbzAAKTyxRnu_nidhAZjnLMGT_WrnfR6R4nO-_xuqDukvOWVg8Ns7eOzvIvrnuktvbM/exec";

        HttpUrl url = HttpUrl.parse(scriptUrl).newBuilder()
            .addQueryParameter("action",          "add_expense")
            .addQueryParameter("user",            userEmail)
            .addQueryParameter("type",            type)
            .addQueryParameter("amount",          String.valueOf(finalAmount))
            .addQueryParameter("date",            selectedDate)
            .addQueryParameter("day",             "")
            .addQueryParameter("category",        refund ? "Refund" : cat)
            .addQueryParameter("original_amount", String.valueOf(amount))
            .addQueryParameter("is_split",        "no")
            .addQueryParameter("split_count",     "1")
            .addQueryParameter("others_total",    "0")
            .addQueryParameter("cc_breakdown",    "")
            .addQueryParameter("is_refund",       refund ? "yes" : "no")
            .build();

        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(QuickAddActivity.this, "Network error — try again", Toast.LENGTH_SHORT).show();
                });
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    // Trigger widget refresh
                    Intent refresh = new Intent(QuickAddActivity.this, WidgetUpdateService.class);
                    refresh.putExtra("force_refresh", true);
                    startService(refresh);

                    Toast.makeText(QuickAddActivity.this,
                        refund ? "↩ Refund saved!" : "✓ Saved " + type, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }
}
