package com.igoy86.nexttranslate;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.databinding.ActivityLanguageBinding;
import com.igoy86.nexttranslate.presentation.base.BaseActivity;
import com.igoy86.nexttranslate.presentation.language.LanguageFragment;

/**
 * LanguageActivity — Thin wrapper hosting LanguageFragment.
 */
public class LanguageActivity extends BaseActivity<ActivityLanguageBinding> {

    @NonNull
    @Override
    protected ActivityLanguageBinding initBinding() {
        return ActivityLanguageBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initViews() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(binding.fragmentContainer.getId(),
                        new LanguageFragment())
                .commit();
    }

    @Override
    protected void initObservers() {}

    @Override
    protected void initListeners() {}
}