package sirius.app.vgui;

import com.google.common.collect.Lists;
import com.vaadin.ui.*;
import org.joda.time.DateTime;
import sirius.kernel.async.Async;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.ValueHolder;
import sirius.kernel.health.Exceptions;
import sirius.kernel.nls.NLS;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 19.01.14
 * Time: 16:49
 * To change this template use File | Settings | File Templates.
 */
public abstract class TextOutApp extends Application {
    protected ValueHolder<Boolean> runFlag = ValueHolder.of(true);
    private Button startButton;
    private Button stopButton;
    private List<String> lines = Lists.newArrayList();
    private TextArea textOut;
    private static final int MAX_LINES = 512;

    @Override
    public void setupUI(GUI gui, VerticalLayout content) {
        HorizontalLayout buttonBar = new HorizontalLayout();
        content.addComponent(buttonBar);

        startButton = new Button("Start");
        stopButton = new Button("Stop");
        startButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                start();
            }
        });
        stopButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                stop();
            }
        });
        stopButton.setEnabled(false);

        buttonBar.addComponent(startButton);
        buttonBar.addComponent(stopButton);

        textOut = new TextArea();
        content.addComponent(textOut);
        textOut.setSizeFull();
        textOut.setReadOnly(true);

        super.setupUI(gui, content);
    }

    private void stop() {
        runFlag.set(false);
        stopButton.setEnabled(false);
    }

    private void start() {
        startButton.setEnabled(false);
        lines.clear();
        Async.executor("user-app").start(new Runnable() {
            @Override
            public void run() {
                try {
                    println("Execution started");
                    execute();
                    println("Execution completed");
                } catch (Throwable e) {
                    final String error = Exceptions.handle(e).getMessage();
                    println("Execution completed: %s", error);
                    getGUI().access(new Runnable() {
                        @Override
                        public void run() {
                            startButton.setEnabled(true);
                            stopButton.setEnabled(false);
                            Notification.show(error);
                            getGUI().push();
                        }
                    });
                    return;
                }
                getGUI().access(new Runnable() {
                    @Override
                    public void run() {
                        startButton.setEnabled(true);
                        stopButton.setEnabled(false);
                        Notification.show("Job completed");
                        getGUI().push();
                    }
                });
            }
        }).execute();
    }

    protected synchronized void println(final String line, final Object... params) {
        String content = NLS.toUserString(new DateTime(), true) + ": " + Strings.apply(line, params);
        lines.add(content);
        while (lines.size() > MAX_LINES) {
            lines.remove(0);
        }
        getGUI().access(new Runnable() {
            @Override
            public void run() {
                textOut.setReadOnly(false);
                textOut.setValue(Strings.join(lines, "\n"));
                textOut.setReadOnly(true);
                textOut.setCursorPosition(textOut.getValue().length());
                getGUI().push();
            }
        });
    }

    protected boolean isRunning() {
        return runFlag.get();
    }

    protected abstract void execute();
}
