package org.wikipedia.history;

import android.app.*;
import android.content.*;
import android.database.*;
import android.net.*;
import android.os.*;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.*;
import android.view.*;
import android.widget.*;
import com.squareup.picasso.*;
import org.wikipedia.*;
import org.wikipedia.page.*;
import org.wikipedia.pageimages.*;

import java.text.*;
import java.util.*;

public class HistoryActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private ListView historyEntryList;
    private View historyEmptyMessage;
    private HistoryEntryAdapter adapter;

    private WikipediaApp app;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = (WikipediaApp)getApplicationContext();

        setContentView(R.layout.activity_history);
        historyEntryList = (ListView) findViewById(R.id.history_entry_list);
        historyEmptyMessage = findViewById(R.id.history_empty_message);

        adapter = new HistoryEntryAdapter(this, null, true);
        historyEntryList.setAdapter(adapter);
        historyEntryList.setEmptyView(historyEmptyMessage);

        historyEntryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HistoryEntry oldEntry = (HistoryEntry)view.getTag();
                HistoryEntry newEntry = new HistoryEntry(oldEntry.getTitle(), HistoryEntry.SOURCE_HISTORY);

                Intent intent = new Intent();
                intent.setClass(HistoryActivity.this, PageActivity.class);
                intent.setAction(PageActivity.ACTION_PAGE_FOR_TITLE);
                intent.putExtra(PageActivity.EXTRA_PAGETITLE, oldEntry.getTitle());
                intent.putExtra(PageActivity.EXTRA_HISTORYENTRY, newEntry);
                startActivity(intent);
            }
        });

        getSupportLoaderManager().initLoader(0, null, this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(
                this,
                Uri.parse(HistoryEntry.PERSISTANCE_HELPER.getBaseContentURI().toString() + "/" + PageImage.PERSISTANCE_HELPER.getTableName()),
                null,
                null,
                null,
                "timestamp DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoaderLoader, Cursor cursorLoader) {
        adapter.swapCursor(cursorLoader);
        invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoaderLoader) {
        adapter.changeCursor(null);
    }

    private class HistoryEntryAdapter extends CursorAdapter {
        public HistoryEntryAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return getLayoutInflater().inflate(R.layout.item_history_entry, viewGroup, false);
        }

        private String getDateString(Date date) {
            return DateFormat.getDateInstance().format(date);
        }

        private int getImageForSource(int source) {
            switch (source) {
                case HistoryEntry.SOURCE_INTERNAL_LINK:
                    return R.drawable.link;
                case HistoryEntry.SOURCE_EXTERNAL_LINK:
                    return R.drawable.external;
                case HistoryEntry.SOURCE_HISTORY:
                    return R.drawable.external;
                case HistoryEntry.SOURCE_SEARCH:
                    return R.drawable.search;
                case HistoryEntry.SOURCE_SAVED_PAGE:
                    return R.drawable.external;
                case HistoryEntry.SOURCE_LANGUAGE_LINK:
                    return R.drawable.link;
                case HistoryEntry.SOURCE_RANDOM:
                    return R.drawable.random;
                case HistoryEntry.SOURCE_MAIN_PAGE:
                    return R.drawable.link;
                default:
                    throw new RuntimeException("Unknown source id encountered");
            }
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView title = (TextView) view.findViewById(R.id.history_title);
            ImageView source = (ImageView) view.findViewById(R.id.history_source);
            ImageView thumbnail = (ImageView) view.findViewById(R.id.history_thumbnail);
            HistoryEntry entry = HistoryEntry.PERSISTANCE_HELPER.fromCursor(cursor);
            title.setText(entry.getTitle().getDisplayText());
            source.setImageResource(getImageForSource(entry.getSource()));
            view.setTag(entry);

            Picasso.with(HistoryActivity.this)
                    .load(cursor.getString(5))
                    .placeholder(R.drawable.ic_pageimage_placeholder)
                    .error(R.drawable.ic_pageimage_placeholder)
                    .into(thumbnail);

            // Check the previous item, see if the times differe enough
            // If they do, display the section header.
            // Always do it this is the first item.
            String curTime, prevTime = "";
            if (cursor.getPosition() != 0) {
                Cursor prevCursor = (Cursor) getItem(cursor.getPosition() - 1);
                HistoryEntry prevEntry = HistoryEntry.PERSISTANCE_HELPER.fromCursor(prevCursor);
                prevTime = getDateString(prevEntry.getTimestamp());
            }
            curTime = getDateString(entry.getTimestamp());
            TextView sectionHeader = (TextView) view.findViewById(R.id.history_section_header_text);
            if (!curTime.equals(prevTime)) {
                sectionHeader.setText(curTime);
                sectionHeader.setVisibility(View.VISIBLE);
            } else {
                sectionHeader.setVisibility(View.GONE);
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_clear_all_history).setEnabled(historyEntryList.getCount() > 0);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_clear_all_history:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.dialog_title_clear_history)
                        .setMessage(R.string.dialog_message_clear_history);
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Clear history!
                        app.getPersister(HistoryEntry.class).deleteAll();
                    }
                });

                builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Uh, do nothing?
                    }
                });
                builder.create().show();
                return true;
            default:
                throw new RuntimeException("Unknown menu item clicked!");
        }
    }
}