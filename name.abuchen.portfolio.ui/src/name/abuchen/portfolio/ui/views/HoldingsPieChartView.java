package name.abuchen.portfolio.ui.views;

import java.util.StringJoiner;

import javax.inject.Inject;

import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser;
import name.abuchen.portfolio.util.ColorConversion;
import name.abuchen.portfolio.util.Dates;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class HoldingsPieChartView extends AbstractFinanceView
{
    @Inject
    private ExchangeRateProviderFactory factory;

    @Override
    protected String getTitle()
    {
        return Messages.LabelStatementOfAssetsHoldings;
    }

    @Override
    protected Control createBody(Composite parent)
    {
        return new EmbeddedBrowser("/META-INF/html/pie.html") //$NON-NLS-1$
                        .createControl(parent, b -> new LoadDataFunction(b, "loadData")); //$NON-NLS-1$
    }

    private static final class JSColors
    {
        private static final int SIZE = 11;
        private static final float STEP = (360.0f / (float) SIZE);

        private static final float HUE = 262.3f;
        private static final float SATURATION = 0.464f;
        private static final float BRIGHTNESS = 0.886f;

        private int nextSlice = 0;

        public String next()
        {
            float brightness = Math.min(1.0f, BRIGHTNESS + (0.05f * (nextSlice / SIZE)));
            return ColorConversion.toHex((HUE + (STEP * nextSlice++)) % 360f, SATURATION, brightness);
        }
    }

    private final class LoadDataFunction extends BrowserFunction
    {
        private static final String ENTRY = "{\"label\":\"%s\"," //$NON-NLS-1$
                        + "\"value\":%s," //$NON-NLS-1$
                        + "\"color\":\"%s\"," //$NON-NLS-1$
                        + "\"caption\":\"%s  %s  (%s)\"," //$NON-NLS-1$
                        + "\"valueLabel\":\"%s\"" //$NON-NLS-1$
                        + "}"; //$NON-NLS-1$

        private LoadDataFunction(Browser browser, String name)
        {
            super(browser, name);
        }

        public Object function(Object[] arguments)
        {
            try
            {
                CurrencyConverter converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
                ClientSnapshot snapshot = ClientSnapshot.create(getClient(), converter, Dates.today());

                StringJoiner joiner = new StringJoiner(",", "[", "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                JSColors colors = new JSColors();

                snapshot.getAssetPositions()
                                .filter(p -> p.getValuation().getAmount() > 0)
                                .sorted((l, r) -> Long.compare(r.getValuation().getAmount(), l.getValuation()
                                                .getAmount())) //
                                .forEach(p -> {
                                    String name = StringEscapeUtils.escapeJson(p.getDescription());
                                    String percentage = Values.Percent2.format(p.getShare());
                                    joiner.add(String.format(ENTRY, name, //
                                                    p.getValuation().getAmount(), //
                                                    colors.next(), //
                                                    name, Values.Money.format(p.getValuation()), percentage, //
                                                    percentage));
                                });

                return joiner.toString();
            }
            catch (Throwable e)
            {
                PortfolioPlugin.log(e);
                return "[]"; //$NON-NLS-1$
            }
        }
    }
}
