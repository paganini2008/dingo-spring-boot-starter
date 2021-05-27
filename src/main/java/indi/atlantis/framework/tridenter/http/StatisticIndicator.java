package indi.atlantis.framework.tridenter.http;

import java.util.Collection;
import java.util.Map;

/**
 * 
 * StatisticIndicator
 *
 * @author Fred Feng
 * @version 1.0
 */
public interface StatisticIndicator {

	Statistic compute(String provider, Request request);

	Collection<Statistic> toCollection(String provider);

	Map<String, Collection<Statistic>> toEntries();

}