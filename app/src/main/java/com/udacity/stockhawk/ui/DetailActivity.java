
package com.udacity.stockhawk.ui;


import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;

import static com.udacity.stockhawk.R.id.change;


public class DetailActivity extends AppCompatActivity {

    @BindView(R.id.symbol)
    TextView symbolTextView;

    @BindView(R.id.price)
    TextView priceTextView;

    @BindView(change)
    TextView changeTextView;
    private String symbol;
    private Map<String, Stock> stockMap;
    private LineChart mChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);

        Intent intent = getIntent();
        symbol = intent.getExtras().getString(getString(R.string.Intent_stock_name));

        Uri uri = Contract.Quote.makeUriForStock(symbol);
        Cursor cursor = getContentResolver().query(
                uri,
                null,
                null, null, null);
        cursor.moveToFirst();
        String stockSymbol = cursor.getString(Contract.Quote.POSITION_SYMBOL);
        Float stockPrice = cursor.getFloat(Contract.Quote.POSITION_PRICE);
        float rawAbsoluteChange = cursor.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
        float percentageChange = cursor.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

        DecimalFormat dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        DecimalFormat dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus.setPositivePrefix("+");
        dollarFormatWithPlus.setMaximumFractionDigits(2);
        dollarFormat.setMaximumFractionDigits(2);
        dollarFormat.setMinimumFractionDigits(2);
        dollarFormatWithPlus.setMinimumFractionDigits(2);
        DecimalFormat percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        percentageFormat.setMaximumFractionDigits(2);
        percentageFormat.setMinimumFractionDigits(2);
        percentageFormat.setPositivePrefix("+");

        String change = dollarFormatWithPlus.format(rawAbsoluteChange);
        String percentage = percentageFormat.format(percentageChange / 100);

        if (PrefUtils.getDisplayMode(this)
                .equals(this.getString(R.string.pref_display_mode_absolute_key))) {
            changeTextView.setText(change);
        } else {
            changeTextView.setText(percentage);
        }

        if (rawAbsoluteChange > 0) {
            changeTextView.setBackgroundResource(R.drawable.percent_change_pill_green);
        } else {
            changeTextView.setBackgroundResource(R.drawable.percent_change_pill_red);
        }


        symbolTextView.setText(stockSymbol);
        priceTextView.setText(dollarFormat.format(stockPrice));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (symbol != null) {
            getSupportActionBar().setTitle(symbol);
            new GetStock().execute(symbol);
            mChart = (LineChart) findViewById(R.id.chart);
        }
    }

    private class GetStock extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... symbol) {
            try {
                stockMap = YahooFinance.get(symbol, true);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (mChart != null) {
                Stock stock = stockMap.get(symbol);
                try {
                    makeGraph(stock.getHistory().size(), stock.getHistory());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void makeGraph(int count, List<HistoricalQuote> historicalQuotes) {


        ArrayList<Entry> entries = new ArrayList<>();

        for (int i = 1; i < 1 + count; i++) {
            entries.add(new Entry(i, historicalQuotes.get(i - 1).getAdjClose().floatValue()));
        }

        LineDataSet set;

        set = new LineDataSet(entries, getString(R.string.historical));
        set.setColor(getResources().getColor(R.color.colorPrimaryDark));

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(set);

        LineData data = new LineData(dataSets);
        data.setValueTextSize(10f);
        mChart.setData(data);
        mChart.animateX(3000);
    }
}