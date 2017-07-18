package io.mrarm.irc;

import android.content.Context;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.chatlib.ChatApi;
import io.mrarm.chatlib.dto.WhoisInfo;
import io.mrarm.irc.util.StatusBarColorBottomSheetDialog;

public class UserBottomSheetDialog {

    private Context mContext;
    private BottomSheetDialog mDialog;
    private ItemAdapter mAdapter;

    private String mNick;
    private String mUser;
    private String mRealName;
    private List<Pair<String, String>> mEntries = new ArrayList<>();

    public UserBottomSheetDialog(Context context) {
        mContext = context;
    }

    public void requestData(String nick, ChatApi connection) {
        setUser(nick, null, null);
        connection.sendWhois(nick, (WhoisInfo info) -> {
            setUser(info.getNick(), info.getUser(), info.getRealName());
            addEntry(R.string.user_hostname, info.getHost());
            if (info.getServer() != null)
                addEntry(R.string.user_server, mContext.getString(R.string.user_server_format, info.getServer(), info.getServerInfo()));
            if (info.getChannels() != null) {
                StringBuilder b = new StringBuilder();
                for (WhoisInfo.ChannelWithNickPrefixes channel : info.getChannels()) {
                    if (b.length() > 0)
                        b.append(mContext.getString(R.string.text_comma));
                    if (channel.getPrefixes() != null)
                        b.append(channel.getPrefixes());
                    b.append(channel.getChannel());
                }
                addEntry(R.string.user_channels, b.toString());
            }
            if (info.getIdleSeconds() > 0)
                addEntry(R.string.user_idle, mContext.getResources().getQuantityString(R.plurals.time_seconds, info.getIdleSeconds(), info.getIdleSeconds()));
            if (info.isOperator())
                addEntry(R.string.user_server_op, mContext.getString(R.string.user_server_op_desc));
            mAdapter.notifyDataSetChanged();
        }, null);
    }

    public void setUser(String nick, String user, String realName) {
        mNick = nick;
        mUser = user;
        mRealName = realName;
        if (mAdapter != null)
            mAdapter.notifyDataSetChanged();
    }

    public void addEntry(int titleId, String value) {
        addEntry(mContext.getString(titleId), value);
    }

    public void addEntry(String title, String value) {
        mEntries.add(new Pair<>(title, value));
        if (mAdapter != null)
            mAdapter.notifyItemInserted(mEntries.size() - 1 + 1);
    }

    private void create() {
        RecyclerView recyclerView = new RecyclerView(mContext);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        recyclerView.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));
        mAdapter = new ItemAdapter();
        recyclerView.setAdapter(mAdapter);

        mDialog = new StatusBarColorBottomSheetDialog(mContext);
        mDialog.setContentView(recyclerView);
        mDialog.getWindow().getDecorView().addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                recyclerView.setMinimumHeight(bottom-top);
            }
        });
    }

    public void show() {
        if (mDialog == null)
            create();
        mDialog.show();
    }

    private class ItemAdapter extends RecyclerView.Adapter {

        public static final int ITEM_HEADER = 0;
        public static final int ITEM_ENTRY = 1;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == ITEM_HEADER) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.dialog_bottom_user_header, parent, false);
                return new HeaderHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.dialog_bottom_user_entry, parent, false);
                return new EntryHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (position == 0)
                ((HeaderHolder) holder).bind();
            else
                ((EntryHolder) holder).bind(mEntries.get(position - 1));
        }

        @Override
        public int getItemCount() {
            return mEntries.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0)
                return ITEM_HEADER;
            return ITEM_ENTRY;
        }

        private class HeaderHolder extends RecyclerView.ViewHolder {
            private TextView mName;
            private TextView mNick;
            private TextView mUser;

            public HeaderHolder(View itemView) {
                super(itemView);
                mName = (TextView) itemView.findViewById(R.id.name);
                mNick = (TextView) itemView.findViewById(R.id.nick);
                mUser = (TextView) itemView.findViewById(R.id.user);
            }

            public void bind() {
                mName.setText(UserBottomSheetDialog.this.mRealName);
                mNick.setText(UserBottomSheetDialog.this.mNick);
                mUser.setText(UserBottomSheetDialog.this.mUser);
            }
        }

        private class EntryHolder extends RecyclerView.ViewHolder {
            private TextView mTitle;
            private TextView mValue;

            public EntryHolder(View itemView) {
                super(itemView);
                mTitle = (TextView) itemView.findViewById(R.id.title);
                mValue = (TextView) itemView.findViewById(R.id.value);
            }

            public void bind(Pair<String, String> entry) {
                mTitle.setText(entry.first);
                mValue.setText(entry.second);
            }
        }

    }

}