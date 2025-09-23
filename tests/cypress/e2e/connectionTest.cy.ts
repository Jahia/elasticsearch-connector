import {deleteConfigValue, editConfigValue, testConnection} from '../fixtures/utils';

describe('Elasticsearch connection tests', () => {
    const success = true;
    const fail = false;

    it('connects successfully', () => {
        const pwd = Cypress.env('ELASTIC_PASSWORD_ENCODED');
        cy.log(`Setting ES password: ${pwd}`);
        editConfigValue('elasticsearchConnector.password', pwd);
        testConnection(success);
    });

    it('fails test connection with invalid config', () => {
        const hostKey = 'elasticsearchConnector.host';

        // Change host to invalidEnv and expect test to fail
        editConfigValue(hostKey, 'test');
        testConnection(fail);

        // Clean-up: change host back to default value ("elasticsearch")
        deleteConfigValue(hostKey);
    });
});
