package com.willchou.dapenti.presenter;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.willchou.dapenti.R;
import com.willchou.dapenti.model.DaPenTi;
import com.willchou.dapenti.model.Database;
import com.woxthebox.draglistview.DragItem;
import com.woxthebox.draglistview.DragItemAdapter;
import com.woxthebox.draglistview.DragListView;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PageOrderActivity extends AppCompatActivity {
    private static final String TAG = "PageOrderActivity";

    private Button applyButton;
    private DragListView dragListView;

    class ItemDetail { String title; long uniqueID; boolean visible; }
    private List<ItemDetail> itemList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page_order);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener((View v) -> onBackPressed());

        setupData();
        setupLayout();
    }

    private void setupData() {
        Database database = Database.getDatabase();

        if (database == null)
            return;

        List<Pair<String, URL>> categories = new ArrayList<>();
        database.getCategories(categories, false);
        List<Pair<String, Boolean>> visibleList = new ArrayList<>();
        database.getCategoryVisible(visibleList);

        int count = 0;
        for (Pair<String, URL> p : categories) {
            ItemDetail id = new ItemDetail();
            id.title = p.first; id.uniqueID = count ++;

            for (Pair<String, Boolean> p2 : visibleList)
                if (p2.first.equals(id.title)) {
                    id.visible = p2.second;
                    break;
                }

            itemList.add(id);
        }
    }

    private void setupLayout() {
        applyButton = findViewById(R.id.apply_button);
        applyButton.setVisibility(View.GONE);
        applyButton.setOnClickListener(view -> saveChanged());

        dragListView = findViewById(R.id.drag_list_view);
        dragListView.getRecyclerView().setVerticalScrollBarEnabled(true);
        dragListView.setLayoutManager(new LinearLayoutManager(this));
        dragListView.setAdapter(new ItemAdapter(), true);
        dragListView.setCanDragHorizontally(false);
        dragListView.setCustomDragItem(new MyDragItem(this, R.layout.page_list_item));
        dragListView.setDragListListener(new DragListView.DragListListenerAdapter() {
            @Override
            public void onItemDragEnded(int fromPosition, int toPosition) {
                if (fromPosition != toPosition && applyButton.getVisibility() != View.VISIBLE)
                    applyButton.setVisibility(View.VISIBLE);
            }
        });
    }

    private void saveChanged() {
        applyButton.setVisibility(View.GONE);

        List<Pair<String, Boolean>> list = new ArrayList<>();
        for (ItemDetail id : itemList)
            list.add(new Pair<>(id.title, id.visible));

        if (Database.getDatabase() != null) {
            Database.getDatabase().updateCategoriesOrderAndVisible(list);
            if (DaPenTi.getDaPenTi() != null)
                DaPenTi.getDaPenTi().prepareCategory(false);
            Snackbar.make(dragListView, "应用成功", Snackbar.LENGTH_SHORT).show();
        } else {
            Snackbar.make(dragListView, "数据库操作失败", Snackbar.LENGTH_SHORT)
                    .show();
            applyButton.setVisibility(View.VISIBLE);
        }
    }

    class ItemAdapter extends DragItemAdapter<ItemDetail, PageOrderActivity.ViewHolder> {
        ItemAdapter() {
            setItemList(itemList);
        }

        @Override
        public void onBindViewHolder(@NonNull PageOrderActivity.ViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);

            ItemDetail id = itemList.get(position);

            holder.itemDetail = id;
            holder.textView.setText(id.title);
            holder.checkBox.setChecked(id.visible);
        }

        @NonNull
        @Override
        public PageOrderActivity.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getBaseContext()).inflate(R.layout.page_list_item, parent, false);
            return new PageOrderActivity.ViewHolder(view, R.id.drag_image, false);
        }

        @Override
        public long getUniqueItemId(int position) {
            return itemList.get(position).uniqueID;
        }
    }

    class ViewHolder extends DragItemAdapter.ViewHolder {
        TextView textView;
        CheckBox checkBox;
        ItemDetail itemDetail;

        ViewHolder(View itemView, int handleResId, boolean dragOnLongPress) {
            super(itemView, handleResId, dragOnLongPress);
            textView = itemView.findViewById(R.id.drag_page_name);
            checkBox = itemView.findViewById(R.id.visible_checkbox);

            checkBox.setOnCheckedChangeListener((compoundButton, b) -> new Exception().printStackTrace());
        }

        @Override
        public void onItemClicked(View view) {
            super.onItemClicked(view);
            checkBox.toggle();

            itemDetail.visible = checkBox.isChecked();

            if (applyButton.getVisibility() != View.VISIBLE)
                applyButton.setVisibility(View.VISIBLE);
        }
    }

    class MyDragItem extends DragItem {
        MyDragItem(Context context, int layoutId) {
            super(context, layoutId);
        }

        @Override
        public void onBindDragView(View clickedView, View dragView) {
            CharSequence text = ((TextView) clickedView.findViewById(R.id.drag_page_name)).getText();
            ((TextView) dragView.findViewById(R.id.drag_page_name)).setText(text);

            boolean checked = ((CheckBox) clickedView.findViewById(R.id.visible_checkbox)).isChecked();
            ((CheckBox) (dragView.findViewById(R.id.visible_checkbox))).setChecked(checked);
        }
    }
}
