package com.igoy86.nexttranslate.presentation.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.igoy86.nexttranslate.databinding.ItemConversationBinding;

/**
 * Adapter for the conversation log RecyclerView in Dialog Mode.
 *
 * <p>Each item shows the original speech text and its translation.
 * User A bubbles are aligned to the right; User B bubbles to the left.</p>
 */
public class ConversationAdapter extends ListAdapter<ConversationItem, ConversationAdapter.ViewHolder> {

    public ConversationAdapter() {
        super(new DiffUtil.ItemCallback<ConversationItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull ConversationItem a, @NonNull ConversationItem b) {
                return a.getId() == b.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull ConversationItem a, @NonNull ConversationItem b) {
                return a.equals(b);
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemConversationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemConversationBinding binding;

        ViewHolder(@NonNull ItemConversationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull ConversationItem item) {
            binding.tvSpeaker.setVisibility(android.view.View.VISIBLE);
            binding.tvSpeaker.setText(item.isUserA() ? "User A" : "User B");
            binding.tvOriginal.setText(item.getOriginalText());
            binding.tvTranslated.setText(item.getTranslatedText());

            final android.widget.FrameLayout.LayoutParams params =
                (android.widget.FrameLayout.LayoutParams) binding.cardBubble.getLayoutParams();
            if (item.isUserA()) {
                binding.tvSpeaker.setGravity(android.view.Gravity.START);
                params.gravity = android.view.Gravity.START;
            } else {
                binding.tvSpeaker.setGravity(android.view.Gravity.END);
                params.gravity = android.view.Gravity.END;
            }
            binding.cardBubble.setLayoutParams(params);
        }
    }
}
