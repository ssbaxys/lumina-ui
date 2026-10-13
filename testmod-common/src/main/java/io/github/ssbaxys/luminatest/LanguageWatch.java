package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.i18n.I18n;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.widget.Widget;

/** Невидимый элемент: пока он в дереве, вызывает действие при каждой смене языка. */
final class LanguageWatch extends Widget<LanguageWatch> {
    private final Runnable onChange;
    private Subscription subscription;
    private String seen;

    LanguageWatch(Runnable onChange) {
        super(null);
        this.onChange = onChange;
        setHitTestable(false);
        size(0, 0);
    }

    @Override
    protected void onAttach() {
        seen = I18n.current();
        subscription = I18n.language().subscribe(code -> {
            if (code != null && !code.equals(seen)) {
                seen = code;
                onChange.run();
            }
        });
    }

    @Override
    protected void onDetach() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }
}
