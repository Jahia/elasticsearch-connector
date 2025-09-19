import Chainable = Cypress.Chainable;

export const testConnection = (): Chainable<boolean> => {
    return cy.apollo({
        queryFile: 'graphql/testConnection.graphql'
    }).then(resp => {
        return resp.data.admin.elasticsearch.testConnection;
    });
};

export const editConfigValue = (key: string, value: string, pid: string = 'org.jahia.modules.elasticsearchConnector') => {
    return cy.apollo({
        mutationFile: 'graphql/editConfigValue.graphql',
        variables: {pid, key, value}
    });
};

export const getConfigValue = (key: string, pid: string = 'org.jahia.modules.elasticsearchConnector') => {
    return cy.apollo({
        mutationFile: 'graphql/getConfigValue.graphql',
        variables: {pid, key}
    }).then(resp => {
        return resp.data.admin.jahia.configuration.value;
    });
};

export const deleteConfigValue = (key: string, pid: string = 'org.jahia.modules.elasticsearchConnector') => {
    return cy.apollo({
        mutationFile: 'graphql/deleteConfigValue.graphql',
        variables: {pid, key}
    });
};
