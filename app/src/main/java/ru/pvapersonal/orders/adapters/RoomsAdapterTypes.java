package ru.pvapersonal.orders.adapters;

import androidx.annotation.StringRes;

import ru.pvapersonal.orders.R;

public enum RoomsAdapterTypes {
    ADMIN(R.string.admin),
    MEMBER(R.string.member),
    NOT_MEMBER(R.string.not_member);
    @StringRes
    public int titleId;
    RoomsAdapterTypes(@StringRes int titleId) {
        this.titleId = titleId;
    }
}
