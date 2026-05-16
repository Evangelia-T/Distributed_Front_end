package com.example.distributed_front_end;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import common.GameInfo;
import common.Request;
import common.Response;

public class MainActivity extends AppCompatActivity {
    private EditText hostInput;
    private EditText portInput;
    private EditText playerIdInput;
    private EditText balanceAmountInput;
    private EditText providerInput;
    private Spinner riskSpinner;
    private Spinner betCategorySpinner;
    private Spinner starsSpinner;
    private Button addBalanceButton;
    private Button showPlayerStatsButton;
    private Button showAllGamesButton;
    private Button searchButton;
    private TextView statusText;
    private TextView emptyText;
    private TextView playerStatsTitle;
    private LinearLayout playerStatsPanel;
    private LinearLayout playerStatsContainer;
    private LinearLayout gamesContainer;

    private final MasterClient masterClient = new MasterClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hostInput = findViewById(R.id.hostInput);
        portInput = findViewById(R.id.portInput);
        playerIdInput = findViewById(R.id.playerIdInput);
        balanceAmountInput = findViewById(R.id.balanceAmountInput);
        providerInput = findViewById(R.id.providerInput);
        riskSpinner = findViewById(R.id.riskSpinner);
        betCategorySpinner = findViewById(R.id.betCategorySpinner);
        starsSpinner = findViewById(R.id.starsSpinner);
        addBalanceButton = findViewById(R.id.addBalanceButton);
        showPlayerStatsButton = findViewById(R.id.showPlayerStatsButton);
        showAllGamesButton = findViewById(R.id.showAllGamesButton);
        searchButton = findViewById(R.id.searchButton);
        statusText = findViewById(R.id.statusText);
        emptyText = findViewById(R.id.emptyText);
        playerStatsTitle = findViewById(R.id.playerStatsTitle);
        playerStatsPanel = findViewById(R.id.playerStatsPanel);
        playerStatsContainer = findViewById(R.id.playerStatsContainer);
        gamesContainer = findViewById(R.id.gamesContainer);

        addBalanceButton.setOnClickListener(view -> addBalance());
        showPlayerStatsButton.setOnClickListener(view -> showPlayerStats());
        showAllGamesButton.setOnClickListener(view -> showAvailableGames());
        searchButton.setOnClickListener(view -> searchGames());
    }

    private void showAvailableGames() {
        ConnectionSettings settings = readConnectionSettings();
        if (settings == null) {
            return;
        }

        Request request = Request.getAllGames();
        setLoading(true, "Loading available games...");

        new Thread(() -> {
            try {
                Response response = masterClient.send(settings.host, settings.port, request);
                runOnUiThread(() -> showGamesResponse(response, "No available games were returned by Master."));
            } catch (Exception e) {
                runOnUiThread(() -> showError("Loading games failed: " + e.getMessage()));
            }
        }).start();
    }

    private void addBalance() {
        ConnectionSettings settings = readConnectionSettings();
        if (settings == null) {
            return;
        }

        String playerId = readRequiredText(playerIdInput, "Player id is required.");
        if (playerId == null) {
            return;
        }

        Double amount = readAmount(balanceAmountInput);
        if (amount == null) {
            return;
        }

        Request request = Request.addBalance(playerId, amount);
        setLoading(true, "Adding balance for " + playerId + "...");

        new Thread(() -> {
            try {
                Response response = masterClient.send(settings.host, settings.port, request);
                runOnUiThread(() -> {
                    setActionButtonsEnabled(true);
                    if (response.isSuccess()) {
                        showStatus(withBackendDetail(
                                getString(R.string.balance_added_message, formatMoney(amount), playerId),
                                response.getMessage()
                        ), StatusTone.SUCCESS);
                        balanceAmountInput.setText("");
                    } else {
                        showStatus(withBackendDetail("Balance could not be added.", response.getMessage()), StatusTone.ERROR);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> showError("Add balance failed: " + e.getMessage()));
            }
        }).start();
    }

    private void showPlayerStats() {
        showPlayerStats(true);
    }

    private void showPlayerStats(boolean showLoading) {
        ConnectionSettings settings = readConnectionSettings();
        if (settings == null) {
            return;
        }

        String playerId = readRequiredText(playerIdInput, "Player id is required.");
        if (playerId == null) {
            return;
        }

        Request request = Request.playerStats(playerId);
        if (showLoading) {
            setLoading(true, "Loading statistics for " + playerId + "...");
        }

        new Thread(() -> {
            try {
                Response response = masterClient.send(settings.host, settings.port, request);
                runOnUiThread(() -> showPlayerStatsResponse(playerId, response, showLoading));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (showLoading) {
                        showError("Loading player statistics failed: " + e.getMessage());
                    }
                });
            }
        }).start();
    }

    private void searchGames() {
        ConnectionSettings settings = readConnectionSettings();
        if (settings == null) {
            return;
        }

        String playerId = readRequiredText(playerIdInput, "Player id is required.");
        if (playerId == null) {
            return;
        }

        String providerName = optionalText(providerInput);
        String riskLevel = selectedFilter(riskSpinner, "Any risk");
        String betCategory = selectedFilter(betCategorySpinner, "Any bet category");
        Integer minStars = selectedStars();

        Request request = Request.searchGames(playerId, providerName, riskLevel, betCategory, minStars);
        setLoading(true, "Searching games...");

        new Thread(() -> {
            try {
                Response response = masterClient.send(settings.host, settings.port, request);
                runOnUiThread(() -> showGamesResponse(response, "No games matched these filters."));
            } catch (Exception e) {
                runOnUiThread(() -> showError("Search failed: " + e.getMessage()));
            }
        }).start();
    }

    private void placeBet(GameInfo game, EditText amountInput) {
        ConnectionSettings settings = readConnectionSettings();
        if (settings == null) {
            return;
        }

        String playerId = readRequiredText(playerIdInput, "Player id is required.");
        if (playerId == null) {
            return;
        }

        Double amount = readAmount(amountInput);
        if (amount == null) {
            return;
        }

        Request request = Request.placeBet(playerId, game.getGameName(), amount);
        setLoading(true, "Placing bet on " + game.getGameName() + "...");

        new Thread(() -> {
            try {
                Response response = masterClient.send(settings.host, settings.port, request);
                runOnUiThread(() -> {
                    setActionButtonsEnabled(true);
                    if (response.isSuccess()) {
                        showStatus(withBackendDetail(
                                getString(R.string.bet_success_message, game.getGameName(), formatMoney(amount)),
                                response.getMessage()
                        ), StatusTone.SUCCESS);
                        amountInput.setText("");
                    } else {
                        showStatus(withBackendDetail(
                                getString(R.string.bet_error_message, game.getGameName()),
                                response.getMessage()
                        ), StatusTone.ERROR);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> showError("Bet failed: " + e.getMessage()));
            }
        }).start();
    }

    private void rateGame(GameInfo game, Spinner ratingSpinner) {
        ConnectionSettings settings = readConnectionSettings();
        if (settings == null) {
            return;
        }

        String playerId = readRequiredText(playerIdInput, "Player id is required.");
        if (playerId == null) {
            return;
        }

        int rating = ratingSpinner.getSelectedItemPosition() + 1;
        Request request = Request.rateGame(playerId, game.getGameName(), rating);
        setLoading(true, "Rating " + game.getGameName() + "...");

        new Thread(() -> {
            try {
                Response response = masterClient.send(settings.host, settings.port, request);
                runOnUiThread(() -> {
                    setActionButtonsEnabled(true);
                    if (response.isSuccess()) {
                        showStatus(withBackendDetail(
                                getString(R.string.rating_success_message, rating, game.getGameName()),
                                response.getMessage()
                        ), StatusTone.SUCCESS);
                    } else {
                        showStatus(withBackendDetail(
                                getString(R.string.rating_error_message, game.getGameName()),
                                response.getMessage()
                        ), StatusTone.ERROR);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> showError("Rating failed: " + e.getMessage()));
            }
        }).start();
    }

    private void showGamesResponse(Response response, String emptyMessage) {
        setActionButtonsEnabled(true);
        showStatus(response.getMessage(), response.isSuccess() ? StatusTone.SUCCESS : StatusTone.ERROR);
        gamesContainer.removeAllViews();

        List<GameInfo> games = response.getGames();
        if (games == null || games.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText(response.isSuccess() ? emptyMessage : response.getMessage());
            return;
        }

        emptyText.setVisibility(View.GONE);
        for (GameInfo game : games) {
            gamesContainer.addView(createGameView(game));
        }
    }

    private void showPlayerStatsResponse(String playerId, Response response, boolean updateStatus) {
        if (updateStatus) {
            setActionButtonsEnabled(true);
            showStatus(response.getMessage(), response.isSuccess() ? StatusTone.SUCCESS : StatusTone.ERROR);
        }

        if (!response.isSuccess()) {
            return;
        }

        playerStatsTitle.setText(getString(R.string.player_stats_title, playerId));
        playerStatsContainer.removeAllViews();

        Map<String, Double> totals = response.getTotals();
        if (totals == null || totals.isEmpty()) {
            playerStatsContainer.addView(createMetaRow("Status", getString(R.string.no_player_stats)));
        } else {
            for (Map.Entry<String, Double> entry : totals.entrySet()) {
                playerStatsContainer.addView(createMetaRow(formatStatLabel(entry.getKey()), formatStatValue(entry.getKey(), entry.getValue())));
            }
        }

        playerStatsPanel.setVisibility(View.VISIBLE);
    }

    private View createGameView(GameInfo game) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundResource(R.drawable.result_panel_background);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        panelParams.setMargins(0, 0, 0, dp(12));
        panel.setLayoutParams(panelParams);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView logoBadge = createLogoBadge(game);
        header.addView(logoBadge);

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        titleParams.setMargins(dp(12), 0, dp(10), 0);

        TextView title = new TextView(this);
        title.setText(game.getGameName());
        title.setTextColor(getColor(R.color.text_primary));
        title.setTextSize(19);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleBlock.addView(title);

        TextView provider = new TextView(this);
        provider.setText(game.getProviderName());
        provider.setTextColor(getColor(R.color.text_secondary));
        provider.setTextSize(14);
        titleBlock.addView(provider);
        header.addView(titleBlock, titleParams);

        header.addView(createRiskChip(game.getRiskLevel()));
        panel.addView(header);

        LinearLayout metaGrid = new LinearLayout(this);
        metaGrid.setOrientation(LinearLayout.VERTICAL);
        metaGrid.setPadding(0, dp(14), 0, dp(14));
        metaGrid.addView(createMetaRow("Rating", game.getStars() + "/5 from " + game.getNoOfVotes() + " votes"));
        metaGrid.addView(createMetaRow("Bet range", formatMoney(game.getMinBet()) + " - " + formatMoney(game.getMaxBet())));
        metaGrid.addView(createMetaRow("Category", game.getBetCategory() + "  Jackpot x" + formatMultiplier(game.getJackpotMultiplier())));
        panel.addView(metaGrid);

        LinearLayout betRow = new LinearLayout(this);
        betRow.setOrientation(LinearLayout.HORIZONTAL);

        EditText amountInput = new EditText(this);
        amountInput.setHint(R.string.bet_amount_hint);
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amountInput.setSingleLine(true);
        amountInput.setBackgroundResource(R.drawable.result_panel_background);
        amountInput.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, dp(52), 1);
        inputParams.setMargins(0, 0, dp(10), 0);
        betRow.addView(amountInput, inputParams);

        Button betButton = new Button(this);
        betButton.setText(R.string.place_bet_button);
        betButton.setOnClickListener(view -> placeBet(game, amountInput));
        betRow.addView(betButton, new LinearLayout.LayoutParams(dp(120), dp(52)));

        panel.addView(betRow);

        LinearLayout ratingRow = new LinearLayout(this);
        ratingRow.setOrientation(LinearLayout.HORIZONTAL);
        ratingRow.setPadding(0, dp(10), 0, 0);

        Spinner ratingSpinner = new Spinner(this);
        ArrayAdapter<CharSequence> ratingAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.rating_options,
                android.R.layout.simple_spinner_item
        );
        ratingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ratingSpinner.setAdapter(ratingAdapter);
        ratingSpinner.setSelection(Math.max(0, Math.min(4, game.getStars() - 1)));
        ratingSpinner.setBackgroundResource(R.drawable.result_panel_background);
        LinearLayout.LayoutParams ratingParams = new LinearLayout.LayoutParams(0, dp(52), 1);
        ratingParams.setMargins(0, 0, dp(10), 0);
        ratingRow.addView(ratingSpinner, ratingParams);

        Button ratingButton = new Button(this);
        ratingButton.setText(R.string.rate_game_button);
        ratingButton.setOnClickListener(view -> rateGame(game, ratingSpinner));
        ratingRow.addView(ratingButton, new LinearLayout.LayoutParams(dp(120), dp(52)));

        panel.addView(ratingRow);
        return panel;
    }

    private TextView createLogoBadge(GameInfo game) {
        TextView badge = new TextView(this);
        badge.setText(initials(game.getGameName()));
        badge.setGravity(android.view.Gravity.CENTER);
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(15);
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badge.setBackground(roundedBackground(getColor(R.color.primary_action), dp(8), 0, 0));
        badge.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));
        return badge;
    }

    private TextView createRiskChip(String riskLevel) {
        int color = riskColor(riskLevel);
        TextView chip = new TextView(this);
        chip.setText(riskLevel == null ? "RISK" : riskLevel.toUpperCase(Locale.US));
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setTextColor(color);
        chip.setTextSize(12);
        chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        chip.setPadding(dp(10), 0, dp(10), 0);
        chip.setBackground(roundedBackground(Color.WHITE, dp(14), color, 1));
        chip.setMinWidth(dp(72));
        chip.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(32)));
        return chip;
    }

    private View createMetaRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(3), 0, dp(3));

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(getColor(R.color.text_secondary));
        labelView.setTextSize(14);
        row.addView(labelView, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(getColor(R.color.text_primary));
        valueView.setTextSize(14);
        valueView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        row.addView(valueView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        return row;
    }

    private String formatStatLabel(String key) {
        if (key == null || key.trim().isEmpty()) {
            return "Value";
        }

        String normalized = key.trim().toLowerCase(Locale.US);
        switch (normalized) {
            case "balance":
                return "Balance";
            case "total_bets":
            case "totalbets":
            case "bets":
                return "Total bets";
            case "total_won":
            case "totalwon":
            case "wins":
                return "Total won";
            case "total_lost":
            case "totallost":
            case "losses":
                return "Total lost";
            case "net_profit":
            case "netprofit":
            case "profit":
                return "Net profit";
            case "games_played":
            case "gamesplayed":
                return "Games played";
            default:
                return titleCase(key.replace('_', ' '));
        }
    }

    private String formatStatValue(String key, Double value) {
        if (value == null) {
            return "-";
        }

        String normalized = key == null ? "" : key.trim().toLowerCase(Locale.US);
        if (normalized.contains("count")
                || normalized.contains("bets")
                || normalized.contains("games")
                || value == Math.rint(value) && !normalized.contains("balance")
                && !normalized.contains("won") && !normalized.contains("lost")
                && !normalized.contains("profit")) {
            return String.format(Locale.US, "%.0f", value);
        }

        return formatMoney(value);
    }

    private String titleCase(String value) {
        String[] parts = value.trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(part.substring(0, 1).toUpperCase(Locale.US));
            if (part.length() > 1) {
                result.append(part.substring(1).toLowerCase(Locale.US));
            }
        }
        return result.length() == 0 ? "Value" : result.toString();
    }

    private GradientDrawable roundedBackground(int fillColor, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) {
            drawable.setStroke(dp(strokeWidth), strokeColor);
        }
        return drawable;
    }

    private int riskColor(String riskLevel) {
        if (riskLevel == null) {
            return getColor(R.color.text_secondary);
        }

        switch (riskLevel.trim().toLowerCase(Locale.US)) {
            case "high":
                return Color.rgb(229, 72, 77);
            case "medium":
                return Color.rgb(217, 119, 6);
            default:
                return Color.rgb(22, 163, 74);
        }
    }

    private String initials(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "?";
        }

        String[] parts = value.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.US);
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.US);
    }

    private String formatMoney(double value) {
        return String.format(Locale.US, "%.2f FUN", value);
    }

    private String formatMultiplier(double value) {
        if (value == Math.rint(value)) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private ConnectionSettings readConnectionSettings() {
        String host = readRequiredText(hostInput, "Master host is required.");
        if (host == null) {
            return null;
        }

        String portText = readRequiredText(portInput, "Master port is required.");
        if (portText == null) {
            return null;
        }

        try {
            int port = Integer.parseInt(portText);
            if (port <= 0 || port > 65535) {
                throw new NumberFormatException();
            }
            return new ConnectionSettings(host, port);
        } catch (NumberFormatException e) {
            portInput.setError("Use a valid TCP port.");
            return null;
        }
    }

    private String readRequiredText(EditText input, String error) {
        String value = input.getText().toString().trim();
        if (value.isEmpty()) {
            input.setError(error);
            return null;
        }
        return value;
    }

    private String optionalText(EditText input) {
        String value = input.getText().toString().trim();
        return value.isEmpty() ? null : value;
    }

    private String selectedFilter(Spinner spinner, String emptyLabel) {
        String selected = spinner.getSelectedItem().toString();
        return selected.equals(emptyLabel) ? null : selected;
    }

    private Integer selectedStars() {
        int position = starsSpinner.getSelectedItemPosition();
        return position == 0 ? null : position;
    }

    private Double readAmount(EditText amountInput) {
        String amountText = amountInput.getText().toString().trim();
        if (amountText.isEmpty()) {
            amountInput.setError("Bet amount is required.");
            return null;
        }

        try {
            double amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                throw new NumberFormatException();
            }
            return amount;
        } catch (NumberFormatException e) {
            amountInput.setError("Use a positive amount.");
            return null;
        }
    }

    private void setLoading(boolean loading, String message) {
        setActionButtonsEnabled(!loading);
        showStatus(message, StatusTone.INFO);
    }

    private void showError(String message) {
        setActionButtonsEnabled(true);
        showStatus(message, StatusTone.ERROR);
    }

    private void setActionButtonsEnabled(boolean enabled) {
        addBalanceButton.setEnabled(enabled);
        showPlayerStatsButton.setEnabled(enabled);
        showAllGamesButton.setEnabled(enabled);
        searchButton.setEnabled(enabled);
    }

    private void showStatus(String message, StatusTone tone) {
        int textColor;
        int backgroundColor;
        int borderColor;

        switch (tone) {
            case SUCCESS:
                textColor = getColor(R.color.success);
                backgroundColor = getColor(R.color.status_success_background);
                borderColor = getColor(R.color.status_success_border);
                break;
            case ERROR:
                textColor = getColor(R.color.error);
                backgroundColor = getColor(R.color.status_error_background);
                borderColor = getColor(R.color.status_error_border);
                break;
            default:
                textColor = getColor(R.color.text_secondary);
                backgroundColor = getColor(R.color.status_info_background);
                borderColor = getColor(R.color.status_info_border);
                break;
        }

        statusText.setText(message);
        statusText.setTextColor(textColor);
        statusText.setBackground(roundedBackground(backgroundColor, dp(8), borderColor, 1));
        statusText.setVisibility(View.VISIBLE);
    }

    private String withBackendDetail(String friendlyMessage, String backendMessage) {
        if (backendMessage == null || backendMessage.trim().isEmpty()) {
            return friendlyMessage;
        }

        String trimmed = backendMessage.trim();
        if (friendlyMessage.equals(trimmed)) {
            return friendlyMessage;
        }

        return friendlyMessage + "\n" + trimmed;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private enum StatusTone {
        INFO,
        SUCCESS,
        ERROR
    }

    private static class ConnectionSettings {
        final String host;
        final int port;

        ConnectionSettings(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }
}
