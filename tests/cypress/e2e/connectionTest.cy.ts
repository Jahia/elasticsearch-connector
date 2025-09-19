import {deleteConfigValue, editConfigValue, getConfigValue, testConnection} from '../fixtures/utils';

describe('Elasticsearch connection tests', () => {
    it('connects successfully', () => {
        testConnection().then(result => {
            assert(result);
        });
    });

    it('fails test connection with invalid config', () => {
        const hostKey = 'elasticsearchConnector.host';
        const invalidHost = 'test';

        // Change host to invalidEnv
        editConfigValue(hostKey, invalidHost)
            .then(() => getConfigValue(hostKey))
            .then(result => {
                expect(result).to.equal(invalidHost);
            });

        testConnection().then(result => {
            assert(!result);
        });

        // Clean-up: change host back to default value ("elasticsearch")
        deleteConfigValue(hostKey);
    });
});
