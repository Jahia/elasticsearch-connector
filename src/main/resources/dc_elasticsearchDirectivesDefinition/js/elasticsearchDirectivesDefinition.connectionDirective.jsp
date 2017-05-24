<%@page contentType="text/javascript" %>
    <%@ taglib prefix="dbc" uri="http://www.jahia.org/dbconnector/functions" %>
    <%--&lt;%&ndash;@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"&ndash;%&gt;--%>
    (function () {
        'use strict';
        var elasticsearchConnectionDirective = function ($log, contextualData, dcTemplateResolver) {
            var directive = {
                restrict: 'E',
                templateUrl: function(el, attrs) {
                    return dcTemplateResolver.resolveTemplatePath('${dbc:addDatabaseConnectorModulePath('/database-connector-directives/elasticsearch-directives', renderContext)}', 'connectionManagement');
                },
                controller: elasticsearchConnectionDirectiveController,
                controllerAs: 'cecc',
                bindToController: {
                    mode: '@',
                    connection: '=',
                    databaseType: '@'
                },
                link: linkFunc
            };

            return directive;

            function linkFunc(scope, el, attr, ctrls) {
            }

        };

        angular
            .module('databaseConnector')
            .directive('elasticsearchConnectionDirective', ['$log', 'contextualData', 'dcTemplateResolver', elasticsearchConnectionDirective]);

        var elasticsearchConnectionDirectiveController = function ($scope, contextualData,
                                                           dcDataFactory, toaster, i18n, $DCSS) {
            var cecc = this;

            cecc.isEmpty = {};
            cecc.spinnerOptions = {
                showSpinner: false,
                mode: 'indeterminate'
            };
            cecc.tabLabels = {
                settings: i18n.message('ec_elasticsearchConnector.label.settings'),
                advancedSettings: i18n.message('ec_elasticsearchConnector.label.advancedSettings')
            };
            cecc.validations = {
                host: {
                    'required': i18n.message('ec_elasticsearchConnector.label.validation.required'),
                    'md-maxlength': i18n.format('ec_elasticsearchConnector.label.validation.maxLength', '15'),
                    'minlength': i18n.format('ec_elasticsearchConnector.label.validation.minLength', '4')
                },
                port: {
                    'pattern': i18n.format('ec_elasticsearchConnector.label.validation.range', '1|65535')
                },
                id: {
                    'required': i18n.message('ec_elasticsearchConnector.label.validation.required'),
                    'connection-id-validator': i18n.message('ec_elasticsearchConnector.label.validation.connectionIdInUse'),
                    'pattern': i18n.message('ec_elasticsearchConnector.label.validation.alphanumeric'),
                    'md-maxlength': i18n.format('ec_elasticsearchConnector.label.validation.minLength', '30')
                },
                dbName: {
                    'required': i18n.message('ec_elasticsearchConnector.label.validation.required'),
                    'pattern': i18n.message('ec_elasticsearchConnector.label.validation.alphanumeric'),
                    'md-maxlength': i18n.format('ec_elasticsearchConnector.label.validation.minLength', '30')
                },
                user: {
                    'pattern': i18n.message('ec_elasticsearchConnector.label.validation.elasticsearch.user'),
                    'minlength': i18n.format('ec_elasticsearchConnector.label.validation.minLength', '4'),
                    'md-maxlength': i18n.format('ec_elasticsearchConnector.label.validation.minLength', '30')
                },
                authDb: {
                    'required': i18n.message('ec_elasticsearchConnector.label.validation.required')
                },
                replicaSet: {
                    name: {
                        'required': i18n.message('ec_elasticsearchConnector.label.validation.required')
                    }
                },
                members: {
                    host: {
                        'required': i18n.message('ec_elasticsearchConnector.label.validation.required')
                    },
                    port: {
                        'pattern': i18n.format('ec_elasticsearchConnector.label.validation.range', '1|65535')
                    }
                },
                connectTimeoutMS: {
                    'pattern' : i18n.message('ec_elasticsearchConnector.label.validation.integer')
                },
                socketTimeoutMS: {
                    'pattern' : i18n.message('ec_elasticsearchConnector.label.validation.integer')
                },
                maxPoolSize: {
                    'pattern' : i18n.message('ec_elasticsearchConnector.label.validation.integer')

                },
                minPoolSize: {
                    'pattern' : i18n.message('ec_elasticsearchConnector.label.validation.integer')

                },
                waitQueueTimeoutMS: {
                    'pattern' : i18n.message('ec_elasticsearchConnector.label.validation.integer')
                }
            };

            cecc.createElasticSearchConnection = createElasticSearchConnection;
            cecc.editElasticSearchConnection = editElasticSearchConnection;
            cecc.testElasticSearchConnection = testElasticSearchConnection;
            cecc.cancel = cancel;
            cecc.updateIsEmpty = updateIsEmpty;
            cecc.updateImportedConnection = updateImportedConnection;
            cecc.initReplicaMember = initReplicaMember;
            cecc.addReplicaMember = addReplicaMember;
            cecc.removeReplicaMember = removeReplicaMember;
            cecc.updateReplicaSetOptions = updateReplicaSetOptions;
            cecc.getMessage = i18n.message;

            cecc.$onInit = function init() {
                var url = contextualData.apiUrl + $DCSS.connectorsMetaData[cecc.databaseType].entryPoint + '/writeconcernoptions';
                dcDataFactory.customRequest({
                    url: url,
                    method: 'GET'
                }).then(function (response) {
                    cecc.spinnerOptions.showSpinner = false;
                    cecc.writeConcernOptions = response;
                }, function (response) {
                    cecc.spinnerOptions.showSpinner = false;
                });
                if (_.isUndefined(cecc.connection.port) || cecc.connection.port == null) {
                    cecc.connection.port = "27017";
                }

                cecc.isEmpty.password = updateIsEmpty('password');
                cecc.isEmpty.user = updateIsEmpty('user');
                if (cecc.mode === 'import-edit') {
                    cecc.connection.oldId = null;
                }
                if (cecc.mode === 'edit') {
                    cecc.connection.oldId = angular.copy(cecc.connection.id);
                } else {
                    cecc.connection.isConnected = true;
                }
                if (_.isUndefined(cecc.connection.options) || cecc.connection.options == null || _.isString(cecc.connection.options) && cecc.connection.options.trim() == '') {
                    cecc.connection.options = {};
                } else if (_.isString(cecc.connection.options)){
                    cecc.connection.options = JSON.parse(cecc.connection.options);
                }
                cecc.isReplicaSet = !_.isUndefined(cecc.connection.options.repl);
                if (_.isUndefined(cecc.connection.options.conn)) {
                    cecc.connection.options.conn = {}
                }
                if (_.isUndefined(cecc.connection.options.connPool)) {
                    cecc.connection.options.connPool = {}
                }
            };

            function createElasticSearchConnection() {
                if (cecc.mode === 'import-edit') {
                    return;
                }
                cecc.spinnerOptions.showSpinner = true;
                var url = contextualData.apiUrl + $DCSS.connectorsMetaData[cecc.databaseType].entryPoint + '/add';

                var data = angular.copy(cecc.connection);
                var options = prepareOptions(data.options);
                if (options == null) {
                    delete data.options;
                } else {
                    data.options = options;
                }
                if(data.user == null || _.isEmpty(data.user)) {
                    data.authDb="";
                    data.password="";
                }
                dcDataFactory.customRequest({
                    url: url,
                    method: 'POST',
                    data: data
                }).then(function (response) {

                    cecc.spinnerOptions.showSpinner = false;
                    $scope.$emit('connectionSuccessfullyCreated', null);
                    showConfirmationToast(response.connectionVerified);
                }, function (response) {
                    cecc.spinnerOptions.showSpinner = false;
                    console.log('error', response);
                    toaster.pop({
                        type: 'error',
                        title: i18n.message('ec_elasticsearchConnector.toast.title.connectionInvalid'),
                        toastId: 'cti',
                        timeout: 3000
                    });
                });
            }

            function editElasticSearchConnection() {
                if (cecc.mode === 'import-edit') {
                    return;
                }
                cecc.spinnerOptions.showSpinner = true;
                var url = contextualData.apiUrl + '/elasticsearch/edit';
                var data = angular.copy(cecc.connection);
                var options = prepareOptions(data.options);
                if (options == null) {
                    delete data.options;
                } else {
                    data.options = options;
                }
                if(data.user == null || _.isEmpty(data.user)) {
                    data.authDb="";
                    data.password="";
                }
                dcDataFactory.customRequest({
                    url: url,
                    method: 'PUT',
                    data: data
                }).then(function (response) {
                    cecc.spinnerOptions.showSpinner = false;
                    $scope.$emit('connectionSuccessfullyCreated', null);
                    showConfirmationToast(response.connectionVerified);
                }, function (response) {
                    cecc.spinnerOptions.showSpinner = false;
                    console.log('error', response);
                    toaster.pop({
                        type: 'error',
                        title: i18n.message('ec_elasticsearchConnector.toast.title.connectionInvalid'),
                        toastId: 'cti',
                        timeout: 3000
                    });
                });
            }

            function testElasticSearchConnection() {
                cecc.spinnerOptions.showSpinner = true;
                var url = contextualData.apiUrl + $DCSS.connectorsMetaData[cecc.databaseType].entryPoint + '/testconnection';
                var data = angular.copy(cecc.connection);
                var options = prepareOptions(data.options);
                if (options == null) {
                    delete data.options;
                } else {
                    data.options = options;
                }
                if(data.user == null || _.isEmpty(data.user)) {
                    data.authDb="";
                    data.password="";
                }
                dcDataFactory.customRequest({
                    url: url,
                    method: 'POST',
                    data: data
                }).then(function (response) {
                    if (response.result) {
                        toaster.pop({
                            type: 'success',
                            title: i18n.message('ec_elasticsearchConnector.toast.title.connectionValid'),
                            toastId: 'ctv',
                            timeout: 3000
                        });
                    } else {
                        toaster.pop({
                            type: 'error',
                            title: i18n.message('ec_elasticsearchConnector.toast.title.connectionInvalid'),
                            toastId: 'cti',
                            timeout: 3000
                        });
                    }
                    cecc.spinnerOptions.showSpinner = false;
                }, function (response) {
                    console.log('error', response);
                    cecc.spinnerOptions.showSpinner = false;
                });
            }

            function cancel() {
                if (cecc.mode === 'import-edit') {
                    $scope.$emit('importConnectionClosed', null);
                } else {
                    $scope.$emit('creationCancelled', null);
                    $DCSS.selectedDatabaseType = null;
                    //@TODO reset display of currently selected directive.
                }
            }

            function updateIsEmpty(property) {
                return cecc.isEmpty[property] = cecc.connection[property] === undefined || cecc.connection[property] === null || (typeof cecc.connection[property] === 'string' && cecc.connection[property].trim().length === 0);
            }

            function showConfirmationToast(verified) {
                if (verified) {
                    toaster.pop({
                        type: 'success',
                        title: i18n.message('ec_elasticsearchConnector.toast.title.connectionSavedSuccessfully'),
                        body: i18n.message('ec_elasticsearchConnector.toast.message.connectionVerificationSuccessful'),
                        toastId: 'cm',
                        timeout: 4000
                    });
                } else {
                    toaster.pop({
                        type: 'warning',
                        title: i18n.message('ec_elasticsearchConnector.toast.title.connectionSavedSuccessfully'),
                        body: i18n.message('ec_elasticsearchConnector.toast.message.connectionVerificationFailed'),
                        toastId: 'cm',
                        timeout: 4000
                    });
                }
            }

            function prepareOptions(options) {
                if (!_.isEmpty(options.repl) && options.repl == null) {
                    if (options.repl.replicaSet == null || (_.isString(options.repl.replicaSet) && options.repl.replicaSet.trim().length == 0)) {
                        delete options.repl.replicaSet
                    }
                    if (_.isEmpty(options.repl.members) || options.repl.members == null) {
                        delete options.repl.members
                    }
                }
                //Check if the repl object is empty after we checked the members
                if (_.isEmpty(options.repl) || options.repl == null) {
                    delete options.repl
                }

                if (options.conn.connectTimeoutMS == null || (_.isString(options.conn.connectTimeoutMS) && options.conn.connectTimeoutMS.trim().length == 0)) {
                    delete options.conn.connectTimeoutMS
                }
                if (options.conn.socketTimeoutMS == null || (_.isString(options.conn.socketTimeoutMS) && options.conn.socketTimeoutMS.trim().length == 0)) {
                    delete options.conn.socketTimeoutMS
                }
                //Check if the conn object is empty after we checked the members
                if (_.isEmpty(options.conn) || options.conn == null) {
                    delete options.conn;
                }

                if (options.connPool.maxPoolSize == null || (_.isString(options.connPool.maxPoolSize) && options.connPool.maxPoolSize.trim().length == 0)) {
                    delete options.connPool.maxPoolSize
                }
                if (options.connPool.minPoolSize == null || (_.isString(options.connPool.minPoolSize) && options.connPool.minPoolSize.trim().length == 0)) {
                    delete options.connPool.minPoolSize
                }
                if (options.connPool.waitQueueTimeoutMS == null || (_.isString(options.connPool.waitQueueTimeoutMS) && options.connPool.waitQueueTimeoutMS.trim().length == 0)) {
                    delete options.connPool.waitQueueTimeoutMS
                }
                //Check if the connPool object is empty after we checked the members
                if (_.isEmpty(options.connPool) || options.connPool == null) {
                    delete options.connPool

                }

                return _.isEmpty(options) ? null : JSON.stringify(options);
            }

            function updateImportedConnection() {
                if(cecc.connection.user == null || _.isEmpty(cecc.connection.user)) {
                    cecc.connection.authDb="";
                    cecc.connection.password="";
                }
                $scope.$emit('importConnectionClosed', cecc.connection);
            }

            function addReplicaMember() {
                if (!_.isUndefined(cecc.connection.options.repl.members)) {
                    cecc.connection.options.repl.members.push({});
                }
            }

            function initReplicaMember() {
                if (!_.isUndefined(cecc.connection.options.repl.members) && _.isEmpty(cecc.connection.options.repl.members)) {

                    cecc.connection.options.repl.members.push({});
                }
            }

            function removeReplicaMember(index) {
                if (!_.isUndefined(cecc.connection.options.repl.members)) {
                    cecc.connection.options.repl.members.splice(index, 1);
                }
            }

            function updateReplicaSetOptions() {
                if (cecc.isReplicaSet) {
                    if (_.isUndefined(cecc.connection.options.repl) || cecc.connection.options.repl == null) {
                        //create replica set object
                        cecc.connection.options.repl = {
                            replicaSet: "",
                            members: []
                        }
                    }
                } else {
                    //remove replicaSetObject
                    delete cecc.connection.options.repl;
                }
            }

        };

        elasticsearchConnectionDirectiveController.$inject = ['$scope', 'contextualData',
            'dcDataFactory', 'toaster', 'i18nService', '$DCStateService'];

    })();
