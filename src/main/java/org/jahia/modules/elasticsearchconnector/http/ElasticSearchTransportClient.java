package org.jahia.modules.elasticsearchconnector.http;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.util.Arrays;

/**
 * Created by stefan on 2017-05-30.
 */
public class ElasticSearchTransportClient extends PreBuiltTransportClient implements TransportClientService {

    public ElasticSearchTransportClient(Settings settings, Class<? extends Plugin>... plugins) {
        super(settings, Arrays.asList(plugins));
    }
}
