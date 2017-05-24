<dc-spinner spinner-mode="cecc.spinnerOptions.mode" show="cecc.spinnerOptions.showSpinner"></dc-spinner>
<md-content layout-padding>
    <md-tabs md-dynamic-height md-border-bottom>
        <md-tab label="{{cecc.tabLabels.settings}}">
            <form name="elasticsearchForm">
                <md-content class="md-padding">
                    <md-checkbox ng-model="cecc.connection.isConnected" aria-label="Automatic startup after Creation"
                                 ng-true-value="true" ng-false-value="false"
                                 class="md-warn md-align-top-left" flex>
                        <span class="ipsum" message-key="ec_elasticsearchConnector.label.connection.enableConnection"></span>
                    </md-checkbox>

                    <md-button class="md-fab md-mini md-success pull-right"
                               ng-click="cecc.testElasticSearchConnection()"
                               ng-disabled="elasticsearchForm.$invalid || elasticsearchAdvancedForm.$invalid">
                        <i class="fa fa-lg fa-check-circle-o"></i>
                        <md-tooltip class="noToolTipAnimation"
                                    md-direction="top"
                                    md-delay="500">
                            <span message-key="ec_elasticsearchConnector.label.connection.testConnection"></span>
                        </md-tooltip>
                    </md-button>
                    <md-input-container class="md-block" flex-gt-sm="1">
                        <label message-key="ec_elasticsearchConnector.label.host"></label>
                        <input title="host" required ng-minlength="4" md-maxlength="15" name="host"
                               ng-model="cecc.connection.host">
                        <div ng-messages="elasticsearchForm.host.$error">
                            <div ng-message-exp="validationName"
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.host"
                                 ng-class="{'showErrorMessages': elasticsearchForm.host.$invalid && elasticsearchForm.host.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>
                    </md-input-container>

                    <md-input-container class="md-block">
                        <label message-key="ec_elasticsearchConnector.label.port"></label>
                        <input title="port" md-maxlength="5" ng-pattern="/[0-9]*/" name="port"
                               ng-model="cecc.connection.port" length-parser length-parser-max="5" range-parser
                               range-max-value="65535">
                        <div ng-messages="elasticsearchForm.port.$error">
                            <div ng-message-exp="validationName"
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.port"
                                 ng-class="{'showErrorMessages': elasticsearchForm.port.$invalid && elasticsearchForm.port.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>
                    </md-input-container>

                    <md-input-container class="md-block">
                        <label message-key="ec_elasticsearchConnector.label.connection.id"></label>
                        <input title="Connection Name" md-maxlength="30" name="id" required
                               ng-pattern="/^[a-zA-Z0-9]+$/"
                               ng-model="cecc.connection.id" original-value="{{cecc.connection.oldId}}"
                               connection-id-validator="{{cecc.databaseType}}">
                        <div ng-messages="elasticsearchForm.id.$error">
                            <div ng-message-exp="validationName"
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.id"
                                 ng-class="{'showErrorMessages': elasticsearchForm.id.$invalid && elasticsearchForm.id.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>
                    </md-input-container>

                    <md-input-container class="md-block">
                        <label message-key="ec_elasticsearchConnector.label.connection.dbName"></label>
                        <input title="Database Name" md-maxlength="30" required ng-pattern="/^[a-zA-Z0-9_]+$/"
                               name="dbName"
                               ng-model="cecc.connection.dbName">
                        <div ng-messages="elasticsearchForm.dbName.$error">
                            <div ng-message-exp="validationName"
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.dbName"
                                 ng-class="{'showErrorMessages': elasticsearchForm.dbName.$invalid && elasticsearchForm.dbName.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>
                    </md-input-container>

                    <md-input-container class="md-block">
                        <label message-key="ec_elasticsearchConnector.label.user"></label>
                        <input title="User" name="user" ng-minlength="4" md-maxlength="30"
                               ng-model="cecc.connection.user"
                               ng-pattern="/^[a-zA-Z0-9_-]*$/"
                               ng-change="cecc.updateIsEmpty('user');">
                        <div ng-messages="elasticsearchForm.user.$error">
                            <div ng-message-exp="validationName"
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.user"
                                 ng-class="{'showErrorMessages': elasticsearchForm.user.$invalid && elasticsearchForm.user.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>
                    </md-input-container>

                    <md-input-container class="md-block" ng-if="!cecc.isEmpty.user">
                        <label message-key="ec_elasticsearchConnector.label.password"></label>
                        <input title="Password" type="password" name="password" ng-model="cecc.connection.password"
                               ng-change="cecc.updateIsEmpty('password');">
                    </md-input-container>

                    <md-input-container class="md-block" ng-if="!cecc.isEmpty.password && !cecc.isEmpty.user">
                        <label message-key="ec_elasticsearchConnector.label.modal.elasticsearch.authenticationDatabase"></label>
                        <input title="Authentication Database" required name="authDb"
                               ng-model="cecc.connection.authDb">
                        <div ng-messages="elasticsearchForm.authDb.$error">
                            <div ng-message-exp="validationName"
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.authDb"
                                 ng-class="{'showErrorMessages': elasticsearchForm.authDb.$invalid && elasticsearchForm.authDb.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>
                    </md-input-container>
                </md-content>
            </form>
        </md-tab>

        <md-tab label="{{cecc.tabLabels.advancedSettings}}">
            <md-content class="md-padding">
                <form name="elasticsearchAdvancedForm">

                    <md-subheader class=" md-no-sticky">
                        <span message-key="ec_elasticsearchConnector.label.modal.elasticsearch.replicaSet"></span>
                        <md-checkbox class="pull-right"
                                     ng-model="cecc.isReplicaSet"
                                     ng-true-value="true"
                                     ng-false-value="false"
                                     ng-change="cecc.updateReplicaSetOptions()">
                        </md-checkbox>
                    </md-subheader>
                    <div ng-if="cecc.isReplicaSet">
                        <md-input-container class="md-block col-md-offset-2">
                            <label message-key="ec_elasticsearchConnector.label.name"></label>
                            <input title="replicaSetName" required
                                   name="replicaSetName"
                                   ng-pattern="/^[a-zA-Z0-9_]+$/"
                                   ng-model="cecc.connection.options.repl.replicaSet">
                            <div ng-messages="elasticsearchAdvancedForm.replicaSetName.$error">
                                <div ng-message-exp='validationName'
                                     ng-repeat="(validationName, validationMessage) in cecc.validations.replicaSet.name"
                                     ng-class="{'showErrorMessages': elasticsearchAdvancedForm.replicaSetName.$invalid && elasticsearchAdvancedForm.replicaSetName.$touched}"
                                     ng-init="cecc.initReplicaMember()">
                                    {{validationMessage}}
                                </div>
                            </div>
                        </md-input-container>
                        <md-list>
                            <md-list-item ng-repeat="member in cecc.connection.options.repl.members track by $index "
                                          layout-margin layout-padding layout-fill layout-wrap="" class="box-wrap2">
                                <md-input-container class="md-block" layout="flex">
                                    <label message-key="ec_elasticsearchConnector.label.host"></label>
                                    <input title="replicaSetHost" class="md-primary" required
                                           name="replicaSetHost_{{$index}}"
                                           ng-model="member.host">
                                    <div ng-messages="elasticsearchAdvancedForm['replicaSetHost_' + $index].$error">
                                        <div ng-message-exp='validationName'
                                             ng-repeat="(validationName, validationMessage) in cecc.validations.members.host"
                                             ng-class="{'showErrorMessages': elasticsearchAdvancedForm['replicaSetHost_' + $index].$invalid && elasticsearchAdvancedForm['replicaSetHost_' + $index].$touched}">
                                            {{validationMessage}}
                                        </div>
                                    </div>
                                </md-input-container>
                                :
                                <md-input-container class="md-block" layout="flex">
                                    <label message-key="ec_elasticsearchConnector.label.port"></label>
                                    <input title="replicaSetPort" class="md-primary"
                                           name="replicaSetPort_{{$index}}"
                                           ng-pattern="/^[0-9]*$/"
                                           ng-model="member.port " length-parser length-parser-max="5" range-parser
                                           range-max-value="65535">
                                    <div ng-messages="elasticsearchAdvancedForm['replicaSetPort_'+ $index].$error">
                                        <div ng-message-exp='validationName'
                                             ng-repeat="(validationName, validationMessage) in cecc.validations.members.port"
                                             ng-class="{'showErrorMessages': elasticsearchAdvancedForm['replicaSetPort_' + $index].$invalid && elasticsearchAdvancedForm['replicaSetPort_' + $index].$touched}">
                                            {{validationMessage}}
                                        </div>
                                    </div>
                                </md-input-container>
                                <md-icon class="md-secondary"
                                         title="Delete connection"
                                         ng-click="cecc.removeReplicaMember($index)">
                                    <i class="fa fa-trash-o"></i>
                                </md-icon>
                            </md-list-item>
                            <md-list-item>
                                <md-icon class="md-secondary"
                                         title="Create new Elastic Search connection"
                                         ng-click="cecc.addReplicaMember()">
                                    <i class="fa fa-plus"
                                       title=" Add  Replica member"></i>
                                </md-icon>
                            </md-list-item>
                        </md-list>
                    </div>
                    <md-divider></md-divider>

                    <md-subheader class="md-no-sticky">
                        <span message-key="ec_elasticsearchConnector.label.modal.connectionSettings"></span>
                    </md-subheader>

                    <md-input-container class="md-block col-md-offset-2">
                        <label message-key="ec_elasticsearchConnector.label.modal.elasticsearch.connectTimeout"></label>
                        <input title="Connect Timeout" name="connectTimeoutMS" range-parser
                               ng-pattern="/^[0-9]*$/"
                               ng-model="cecc.connection.options.conn.connectTimeoutMS">
                        <div ng-messages="elasticsearchAdvancedForm.connectTimeoutMS.$error">
                            <div ng-message-exp='validationName'
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.connectTimeoutMS"
                                 ng-class="{'showErrorMessages': elasticsearchAdvancedForm.connectTimeoutMS.$invalid && elasticsearchAdvancedForm.connectTimeoutMS.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>
                    </md-input-container>
                    <md-input-container class="md-block col-md-offset-2">
                        <label message-key="ec_elasticsearchConnector.label.modal.elasticsearch.socketTimeout"></label>
                        <input title="Socket Timeout" name="socketTimeoutMS" range-parser
                               ng-pattern="/^[0-9]*$/"
                               ng-model="cecc.connection.options.conn.socketTimeoutMS">
                        <div ng-messages="elasticsearchAdvancedForm.socketTimeoutMS.$error">
                            <div ng-message-exp='validationName'
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.socketTimeoutMS"
                                 ng-class="{'showErrorMessages': elasticsearchAdvancedForm.socketTimeoutMS.$invalid && elasticsearchAdvancedForm.socketTimeoutMS.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>

                    </md-input-container>
                    <md-input-container class="md-block col-md-offset-2">
                        <label message-key="ec_elasticsearchConnector.label.modal.elasticsearch.writeConcern"></label>
                        <md-select ng-model="cecc.connection.writeConcern">
                            <md-option ng-repeat="writeConcern in cecc.writeConcernOptions" value="{{writeConcern}}">
                                {{writeConcern}}
                            </md-option>
                        </md-select>
                    </md-input-container>
                    <md-divider></md-divider>

                    <md-subheader class="md-no-sticky"><span message-key="ec_elasticsearchConnector.label.modal.elasticsearch.connectionPoolSettings"></span>
                    </md-subheader>

                    <md-input-container class="md-block col-md-offset-2">
                        <label message-key="ec_elasticsearchConnector.label.modal.elasticsearch.maxPoolSize"></label>
                        <input title="Max Pool Size" name="maxPoolSize" range-parser
                               ng-pattern="/^[0-9]*$/"
                               ng-model="cecc.connection.options.connPool.maxPoolSize">
                        <div ng-messages="elasticsearchAdvancedForm.maxPoolSize.$error">
                            <div ng-message-exp='validationName'
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.maxPoolSize"
                                 ng-class="{'showErrorMessages': elasticsearchAdvancedForm.maxPoolSize.$invalid && elasticsearchAdvancedForm.maxPoolSize.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>
                    </md-input-container>
                    <md-input-container class="md-block col-md-offset-2">
                        <label message-key="ec_elasticsearchConnector.label.modal.elasticsearch.minPoolSize"></label>
                        <input title="Min Pool Size" name="minPoolSize" range-parser
                               ng-pattern="/^[0-9]*$/"
                               ng-model="cecc.connection.options.connPool.minPoolSize">
                        <div ng-messages="elasticsearchAdvancedForm.minPoolSize.$error">
                            <div ng-message-exp='validationName'
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.minPoolSize"
                                 ng-class="{'showErrorMessages': elasticsearchAdvancedForm.minPoolSize.$invalid && elasticsearchAdvancedForm.minPoolSize.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>
                    </md-input-container>
                    <md-input-container class="md-block col-md-offset-2">
                        <label message-key="ec_elasticsearchConnector.label.modal.elasticsearch.waitQueueTimeout"></label>
                        <input title="Wait Queue Timeout" name="waitQueueTimeoutMS" range-parser
                               ng-pattern="/^[0-9]*$/"
                               ng-model="cecc.connection.options.connPool.waitQueueTimeoutMS">
                        <div ng-messages="elasticsearchAdvancedForm.waitQueueTimeoutMS.$error">
                            <div ng-message-exp='validationName'
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.waitQueueTimeoutMS"
                                 ng-class="{'showErrorMessages': elasticsearchAdvancedForm.waitQueueTimeoutMS.$invalid && elasticsearchAdvancedForm.waitQueueTimeoutMS.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>
                    </md-input-container>
                </form>
            </md-content>
        </md-tab>
    </md-tabs>
    <md-button ng-if="cecc.mode=='create'"
               class="md-raised md-primary pull-right"
               ng-click="cecc.createElasticSearchConnection()"
               ng-disabled="elasticsearchForm.$invalid || elasticsearchAdvancedForm.$invalid"
               message-key="ec_elasticsearchConnector.label.create">
    </md-button>
    <md-button ng-if="cecc.mode=='edit'"
               class="md-raised md-primary pull-right"
               ng-click="cecc.editElasticSearchConnection()"
               ng-disabled="elasticsearchForm.$invalid || elasticsearchAdvancedForm.$invalid"
               message-key="ec_elasticsearchConnector.label.update">
    </md-button>
    <md-button class="md-raised md-primary pull-right"
               ng-disabled="elasticsearchForm.$invalid || elasticsearchAdvancedForm.$invalid"
               ng-if="cecc.mode=='import-edit'"
               ng-click="cecc.updateImportedConnection()"
               message-key="ec_elasticsearchConnector.label.ok">
    </md-button>
    <md-button class="md-raised md-primary pull-right"
               ng-click="cecc.cancel()">
        <span ng-if="cecc.mode=='edit' || cecc.mode=='create'"
              message-key="ec_elasticsearchConnector.label.back"></span>
        <span ng-if="cecc.mode=='import-edit'"
              message-key="ec_elasticsearchConnector.label.cancel"></span>
    </md-button>
</md-content>