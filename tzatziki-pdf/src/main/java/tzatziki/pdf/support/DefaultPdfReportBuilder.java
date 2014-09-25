package tzatziki.pdf.support;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Section;
import gutenberg.itext.*;
import gutenberg.itext.support.FirstPageRenderer;
import gutenberg.util.Consumer;
import tzatziki.analysis.exec.model.FeatureExec;
import tzatziki.analysis.exec.model.ResultExec;
import tzatziki.analysis.exec.model.StepExec;
import tzatziki.analysis.exec.support.TagView;
import tzatziki.analysis.exec.support.TagViews;
import tzatziki.analysis.tag.TagDictionary;
import gutenberg.itext.model.Markdown;
import tzatziki.pdf.model.Steps;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="http://twitter.com/aloyer">@aloyer</a>
 */
public class DefaultPdfReportBuilder {

    public enum Overview {
        FeatureSummary,
        TagDictionary,
        TagViews
    }

    //
    private Configuration configuration = new Configuration();
    private Consumer<PdfReport> configurator;
    private SimpleEmitter coverPage;
    private TagDictionary tagDictionary;
    private TagViews tagViews = new TagViews();
    private List<FeatureExec> features = Lists.newArrayList();
    //
    private List<Overview> overviews = Lists.newArrayList(Overview.FeatureSummary, Overview.TagDictionary, Overview.TagViews);
    //
    private Properties l10n;

    private List<Consumer<PdfReport>> fragments = Lists.newArrayList();


    public DefaultPdfReportBuilder using(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    public DefaultPdfReportBuilder configurator(Consumer<PdfReport> configurator) {
        this.configurator = configurator;
        return this;
    }

    public DefaultPdfReportBuilder noOverview() {
        return this;
    }

    public DefaultPdfReportBuilder defaultOverview() {
        return overview(Overview.values());
    }

    public DefaultPdfReportBuilder overview(Overview... overviews) {
        this.overviews = Arrays.asList(overviews);
        fragments.add(new Consumer<PdfReport>() {
            @Override
            public void consume(PdfReport report) {
                emitOverview(report);
            }
        });
        return this;
    }

    public DefaultPdfReportBuilder title(String title) {
        configuration.declareProperty(Configuration.TITLE, title);
        return this;
    }

    public DefaultPdfReportBuilder subTitle(String subTitle) {
        configuration.declareProperty(Configuration.SUB_TITLE, subTitle);
        return this;
    }

    public DefaultPdfReportBuilder tagDictionary(TagDictionary tagDictionary) {
        this.tagDictionary = tagDictionary;
        return this;
    }

    public DefaultPdfReportBuilder coverPage(SimpleEmitter coverPage) {
        this.coverPage = coverPage;
        return this;
    }

    public DefaultPdfReportBuilder tagViews(TagView... tagViews) {
        this.tagViews.addAll(tagViews);
        return this;
    }

    public DefaultPdfReportBuilder features(final Collection<? extends FeatureExec> features) {
        this.features.addAll(features);
        this.fragments.add(new Consumer<PdfReport>() {
            @Override
            public void consume(PdfReport report) {
                for (FeatureExec feature : features) {
                    emitFeature(report, feature);
                }
            }
        });
        return this;
    }

    public DefaultPdfReportBuilder sampleSteps() {
        this.fragments.add(new Consumer<PdfReport>() {
            @Override
            public void consume(PdfReport report) {
                emitSampleSteps(report);
            }
        });
        return this;
    }

    public DefaultPdfReportBuilder markup(final Markdown markdown) {
        this.fragments.add(new Consumer<PdfReport>() {
            @Override
            public void consume(PdfReport report) {
                emitMarkup(report, markdown);
            }
        });
        return this;
    }

    protected String l10n(String key, Object... args) {
        if (l10n == null) {
            l10n = initL10n();
        }
        String localized = l10n.getProperty(key);
        return MessageFormat.format(localized, args);
    }

    protected Properties initL10n() {
        Properties p = new Properties();

        // sample-steps
        p.setProperty("sample-steps.section-title", "Sample Steps");
        p.setProperty("sample-steps.step-1.keyword", "Given");
        p.setProperty("sample-steps.step-1.sentence", "a passed step");
        p.setProperty("sample-steps.step-2.keyword", "And");
        p.setProperty("sample-steps.step-2.sentence", "a failed step");
        p.setProperty("sample-steps.step-3.keyword", "When");
        p.setProperty("sample-steps.step-3.sentence", "a pending step");
        p.setProperty("sample-steps.step-4.keyword", "But");
        p.setProperty("sample-steps.step-4.sentence", "an undefined step");
        p.setProperty("sample-steps.step-5.keyword", "Then");
        p.setProperty("sample-steps.step-5.sentence", "a skipped step");

        // overview
        p.setProperty("overview.section-title", "Overview");
        p.setProperty("overview.subsection.features.title", "Features");
        p.setProperty("overview.subsection.tags.title", "Tags");
        p.setProperty("overview.subsection.tagViews.title", "Consolidated tag views");

        //
        p.setProperty("header.firstPageTemplate", "");
        p.setProperty("header.otherPageTemplate", "");
        p.setProperty("footer.firstPageTemplate", "");
        p.setProperty("footer.otherPageTemplate", "{0} | $'{'chapterTitle'}'");

        return p;
    }

    public void generate(File output) throws FileNotFoundException, DocumentException {
        checkForRequiredParameters();

        PdfReport report = new PdfReport(configuration);
        if (configurator != null)
            configurator.consume(report);
        report.startReport(output);

        HeaderFooter headerFooter = registerHeaderAndFooter(report);
        emitCoverPage(report);
        report.startContent();

        for (Consumer<PdfReport> fragment : fragments) {
            fragment.consume(report);
        }

        report.endReport(new TableOfContentsPostProcessor(headerFooter));
    }

    protected void checkForRequiredParameters() {
    }

    private void emitSampleSteps(PdfReport report) {
        report.emit(new SimpleEmitter() {
            @Override
            public void emit(ITextContext emitterContext) {
                String p = "sample-steps.";
                Sections sections = emitterContext.sections();
                Section section = sections.newSection(l10n(p + "section-title"), 1, false);
                try {
                    //
                    List<StepExec> list = Arrays.asList(
                            new StepExec(l10n(p + "step-1.keyword"), l10n(p + "step-1.sentence")).declareResult(new ResultExec("passed", null, null, 500L)),
                            new StepExec(l10n(p + "step-2.keyword"), l10n(p + "step-2.sentence")).declareResult(new ResultExec("failed", null, null, 230L)),
                            new StepExec(l10n(p + "step-3.keyword"), l10n(p + "step-3.sentence")).declareResult(new ResultExec("pending", null, null, null)),
                            new StepExec(l10n(p + "step-4.keyword"), l10n(p + "step-4.sentence")).declareResult(new ResultExec("undefined", null, null, null)),
                            new StepExec(l10n(p + "step-5.keyword"), l10n(p + "step-5.sentence")).declareResult(new ResultExec("skipped", null, null, null))
                    );
                    emitterContext.emit(Steps.class, new Steps(list));
                } finally {
                    sections.leaveSection(1);
                }
                emitterContext.append(section);
            }
        });
    }

    private void emitFeature(PdfReport report, FeatureExec feature) {
        report.emit(feature);
    }

    private void emitOverview(PdfReport report) {
        if (overviews.isEmpty())
            return;

        for (FeatureExec feature : features) {
            tagViews.consolidateView(feature);
        }

        report.emit(new SimpleEmitter() {
            @Override
            public void emit(ITextContext emitterContext) {
                Sections sections = emitterContext.sections();
                Section section = sections.newSection(l10n("overview.section-title"), 1);
                try {
                    for (Overview overview : overviews) {
                        switch (overview) {

                            case FeatureSummary:
                                sections.newSection(l10n("overview.subsection.features.title"), 2);
                                emitterContext.emit(new FeatureSummaryListOfSection(features, 3));
                                sections.leaveSection(2);
                                break;
                            case TagDictionary:
                                sections.newSection(l10n("overview.subsection.tags.title"), 2);
                                emitterContext.emit(tagDictionary);
                                sections.leaveSection(2);
                                break;
                            case TagViews:
                                sections.newSection(l10n("overview.subsection.tagViews.title"), 2);
                                emitterContext.emit(tagViews);
                                sections.leaveSection(2);
                                break;
                        }
                    }

                } finally {
                    sections.leaveSection(1);
                }
                emitterContext.append(section);
            }
        });
    }

    private void emitMarkup(PdfReport report, Markdown markdown) {
        if (markdown != null)
            report.emit(markdown);
    }

    private void emitCoverPage(PdfReport report) {
        if (coverPage != null) {
            report.emit(coverPage);
            report.newPage();
            return;
        }

        String title = configuration.getProperty(Configuration.TITLE);
        String subTitle = configuration.getProperty(Configuration.SUB_TITLE);
        if (title != null) {
            FirstPageRenderer coverPage = new FirstPageRenderer(title, subTitle);
            report.emit(coverPage);
            report.newPage();
        }
    }

    private HeaderFooter registerHeaderAndFooter(PdfReport report) {
        ITextContext context = report.iTextContext();
        PageNumber pageNumber = context.pageNumber();
        Styles styles = context.styles();

        String title = configuration.getProperty(Configuration.HEADER_TITLE);
        if(title == null)
            title = configuration.getProperty(Configuration.TITLE);

        Function<PageInfos, Phrase> header = HeaderFooter.create(
                styles,
                HeaderFooter.FOOTER_FONT,
                l10n("header.firstPageTemplate", title),
                l10n("header.otherPageTemplate", title),
                null);
        Function<PageInfos, Phrase> footer = HeaderFooter.create(
                styles,
                HeaderFooter.FOOTER_FONT,
                l10n("footer.firstPageTemplate", title),
                l10n("footer.otherPageTemplate", title),
                null);
        HeaderFooter headerFooter = new HeaderFooter(pageNumber, styles, header, footer);

        context.getPdfWriter().setPageEvent(headerFooter);
        return headerFooter;
    }

}