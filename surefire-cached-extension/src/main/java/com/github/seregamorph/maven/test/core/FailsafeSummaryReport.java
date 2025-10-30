package com.github.seregamorph.maven.test.core;

import com.github.seregamorph.maven.test.util.XmlUtils;
import java.io.File;
import javax.annotation.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * failsafe report file failsafe-reports/failsafe-summary.xml ("failsafe-summary" tag)
 *
 * @author Sergey Chernov
 */
public final class FailsafeSummaryReport {

    private final int completed;
    private final int errors;
    private final int failures;
    private final int skipped;
    private final int flakes;
    @Nullable
    private final String failureMessage;
    private final boolean timeout;

    public FailsafeSummaryReport(
        int completed,
        int errors,
        int failures,
        int skipped,
        int flakes,
        @Nullable String failureMessage,
        boolean timeout
    ) {
        this.completed = completed;
        this.errors = errors;
        this.failures = failures;
        this.skipped = skipped;
        this.flakes = flakes;
        this.failureMessage = failureMessage;
        this.timeout = timeout;
    }

    public static FailsafeSummaryReport fromFile(File file) {
        Document root = XmlUtils.parseXml(file);
        Element rootElement = root.getDocumentElement();
        String tagName = rootElement.getTagName();
        if (!"failsafe-summary".equals(tagName)) {
            throw new IllegalArgumentException("Not a failsafe-summary report file: "
                + file + " with root element '" + tagName + "'");
        }

        int completed = getIntTag(file, rootElement, "completed");
        int errors = getIntTag(file, rootElement, "errors");
        int failures = getIntTag(file, rootElement, "failures");
        int skipped = getIntTag(file, rootElement, "skipped");
        // this field was added only in 3.5.2
        int flakes = getIntTag(file, rootElement, "flakes", 0);

        Element failureMessageElement = getTag(rootElement, "failureMessage");
        String failureMessage = failureMessageElement == null ? null : failureMessageElement.getTextContent();

        boolean timeout = "true".equals(rootElement.getAttribute("timeout"));

        return new FailsafeSummaryReport(completed, errors, failures, skipped, flakes, failureMessage, timeout);
    }

    public int getCompleted() {
        return completed;
    }

    public int getErrors() {
        return errors;
    }

    public int getFailures() {
        return failures;
    }

    public int getSkipped() {
        return skipped;
    }

    public int getFlakes() {
        return flakes;
    }

    @Nullable
    public String getFailureMessage() {
        return failureMessage;
    }

    public boolean isTimeout() {
        return timeout;
    }

    private static int getIntTag(File file, Element rootElement, String tagName) {
        Element tag = getTag(rootElement, tagName);
        if (tag == null) {
            throw new IllegalArgumentException("No '" + tagName + "' tag in " + file);
        }
        return Integer.parseInt(tag.getTextContent());
    }

    private static int getIntTag(File file, Element rootElement, String tagName, int defaultValue) {
        Element tag = getTag(rootElement, tagName);
        if (tag == null) {
            return defaultValue;
        }
        return Integer.parseInt(tag.getTextContent());
    }

    @Nullable
    private static Element getTag(Element rootElement, String tagName) {
        NodeList tagList = rootElement.getElementsByTagName(tagName);
        if (tagList.getLength() == 0) {
            return null;
        }
        return (Element) tagList.item(0);
    }

    @Override
    public String toString() {
        return "FailsafeSummaryReport{" +
            "completed=" + completed +
            ", errors=" + errors +
            ", failures=" + failures +
            ", skipped=" + skipped +
            ", flakes=" + flakes +
            ", failureMessage='" + failureMessage + '\'' +
            ", timeout=" + timeout +
            '}';
    }
}
