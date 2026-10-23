package app.simple.positional.decorations.searchview;

import android.view.View;
import org.jetbrains.annotations.NotNull;

public interface SearchViewEventListener {
    default void onSearchMenuPressed(@NotNull View button) {
    }

    void onSearchTextChanged(@NotNull String keywords, int count);
}
