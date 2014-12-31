package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.imageio.spi.ServiceRegistry;
import javax.inject.Singleton;

import org.eclipse.e4.core.di.annotations.Creatable;

@Singleton
@Creatable
public class ExchangeRateProviderFactory
{
    private final List<ExchangeRateProvider> providers;

    public ExchangeRateProviderFactory()
    {
        providers = new ArrayList<ExchangeRateProvider>();
        Iterator<ExchangeRateProvider> registeredProvider = ServiceRegistry.lookupProviders(ExchangeRateProvider.class);
        while (registeredProvider.hasNext())
            providers.add(registeredProvider.next());
    }

    public List<ExchangeRateProvider> getProviders()
    {
        return Collections.unmodifiableList(providers);
    }

    public List<ExchangeRateTimeSeries> getAvailableTimeSeries()
    {
        List<ExchangeRateTimeSeries> series = new ArrayList<ExchangeRateTimeSeries>();
        for (ExchangeRateProvider p : providers)
            series.addAll(p.getAvailableTimeSeries());
        return series;
    }

    public ExchangeRateTimeSeries getTimeSeries(String baseCurrency, String termCurrency)
    {
        // TODO pick exchange rate with best quality of service
        for (ExchangeRateProvider p : providers)
        {
            ExchangeRateTimeSeries s = p.getTimeSeries(baseCurrency, termCurrency);
            if (s != null)
                return s;
        }

        return null;
    }
}