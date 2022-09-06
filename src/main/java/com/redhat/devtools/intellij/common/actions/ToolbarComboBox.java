package com.redhat.devtools.intellij.common.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;

import javax.swing.JComponent;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ToolbarComboBox<T> extends ComboBoxAction {

    private T selected;
    private final List<T> all;
    private final Function<T, String> itemToLabel;
    private final BiPredicate<T, T> itemEquals;
    private final Consumer<T> onSelect;
    private ComboBoxButton button;

    public ToolbarComboBox(List<T> items, T selected, Function<T, String> itemToLabel, BiPredicate<T, T> itemEquals, Consumer<T> onSelect) {
        this.all = items;
        this.selected = selected;
        this.itemToLabel = itemToLabel;
        this.itemEquals = itemEquals;
        this.onSelect = onSelect;
    }

    @Override
    public DefaultActionGroup createPopupActionGroup(JComponent button) {
        DefaultActionGroup group = new DefaultActionGroup();
        group.addAll(createActions(selected, all));
        return group;
    }

    @Override
    public ComboBoxButton createComboBoxButton(Presentation presentation) {
        this.button = super.createComboBoxButton(presentation);
        setButtonLabel(selected);
        return button;
    }

    public void setSelected(T item) {
        this.selected = item;
        setButtonLabel(item);
    }

    private List<AnAction> createActions(T selected, List<T> all) {
        return all.stream()
                .map(container -> new SelectItemAction(selected, container))
                .collect(Collectors.toList());
    }

    private void setButtonLabel(T item) {
        if (button != null) {
            this.button.getPresentation().setText(itemToLabel.apply(item));
        }
    }

    private class SelectItemAction extends AnAction {

        private final T selected;
        private final T item;

        private SelectItemAction(T selected, T item) {
            super(itemToLabel.apply(item));
            this.selected = selected;
            this.item = item;
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            if (itemEquals.test(item, selected)) {
                // don't perform if item already is current
                return;
            }
            onSelect.accept(item);
            setSelected(item);
        }
    }
}
