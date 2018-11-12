import React, {Component} from 'react';

import {BrowserRouter as Router, Route, Link, Switch, withRouter, Redirect} from 'react-router-dom';
import queryString from 'query-string';

import {TopHeader} from "./pages/TopHeader";
import {Home} from "./pages/Consents";
import './pages/app.css';
import {OrganisationPage} from "./pages/OrganisationPage";
import {OrganisationsPage} from "./pages/OrganisationsPage";
import {SampleConsentsPage} from "./pages/SampleConsentsPage";
import {OrganisationVersionPage} from "./pages/OrganisationVersionPage";
import {UsersPage} from "./pages/UsersPage";

import {ConsentFactPage} from "./pages/ConsentFactPage";
import {UploadFilePage} from "./pages/UploadFilePage";
import {AccountsPage} from "./pages/AccountsPage";
import {AccountPage} from "./pages/AccountPage";
import {ApiKeysPage} from "./pages/ApiKeysPage";
import {ApiKeyPage} from "./pages/ApiKeyPage";

export class NioApp extends Component {

    state = {
        currentActive: 'organisation'
    };

    decorate = (Component, props, currentActive) => {
        const newProps = {...props};
        newProps.location.query = queryString.parse((props.location || {search: ''}).search) || {};
        newProps.params = newProps.match.params || {};

        if (this.state.currentActive !== currentActive)
            this.setState({currentActive});

        return (
            <Component
                {...newProps}
            />
        );
    };

    componentDidMount() {
        this.props.history.listen(() => {
            $('#sidebar').collapse('hide');
            $('#navbar').collapse('hide');
        });
    }

    goToOrganisations = () => {
        this.props.history.push("/organisations");
    };

    render() {
        return (
            <div className="nio-container container-fluid">
                <TopHeader tenant={this.props.tenant} logoutUrl={this.props.logoutUrl}
                           userEmail={this.props.userEmail} securityDefault={this.props.securityDefault} securityAuth0={this.props.securityAuth0}/>
                <div className="container-fluid">
                    <div className="row">
                        <div className="analytics-viewer-bottom-container"
                             style={{display: 'flex', flexDirection: 'row', width: '100%', height: '100%'}}>
                            <div className="col-sm-2 sidebar" id="sidebar">
                                <ul className="nav nav-sidebar">
                                    <li>
                                        <Link to="">
                                            <h3 style={{marginTop: 0, marginLeft: 0}}>
                                                <i className="fa fa-tachometer"/> Accueil
                                            </h3>
                                        </Link>
                                    </li>
                                    <li className={
                                        this.state.currentActive === "organisation" ?
                                            "active"
                                            :
                                            ""}>
                                        <Link to="/organisations">
                                            Organisations
                                        </Link>
                                    </li>
                                    <li className={this.state.currentActive === "user" ? "active" : ""}>
                                        <Link to="/users">
                                            Utilisateurs
                                        </Link>
                                    </li>
                                    <li className={this.state.currentActive === "consent" ? "active" : ""}>
                                        <Link to="/consent">
                                            Consentement
                                        </Link>
                                    </li>
                                </ul>
                            </div>
                            <div className="col-xs-12 col-sm-10 col-sm-offset-2 backoffice-container">
                                <Switch>
                                    <Route exact path="/" component={props => this.decorate(OrganisationsPage, {
                                        ...this.props,
                                        props
                                    }, "organisation")}/>
                                    <Route exact path="/organisations"
                                           component={props => this.decorate(OrganisationsPage, {
                                               ...this.props,
                                               props
                                           }, "organisation")}/>
                                    <Route exact path="/organisations/new"
                                           component={props => this.decorate(OrganisationPage, {
                                               ...this.props,
                                               onSave: this.goToOrganisations,
                                               props
                                           }, "organisation")}/>
                                    <Route exact path="/organisations/:organisationKey"
                                           component={props => this.decorate(OrganisationVersionPage, {
                                               ...this.props,
                                               organisationKey: props.match.params.organisationKey,
                                               onSave: this.goToOrganisations,
                                               props
                                           }, "organisation")}/>
                                    <Route exact path="/organisations/:organisationKey/users"
                                           component={props => this.decorate(UsersPage, {
                                               ...this.props,
                                               organisationKey: props.match.params.organisationKey,
                                               props
                                           }, "user")}/>
                                    <Route exact path="/organisations/:organisationKey/users/:userId"
                                           component={props => this.decorate(ConsentFactPage, {
                                               ...this.props,
                                               organisationKey: props.match.params.organisationKey,
                                               userId: props.match.params.userId,
                                               props
                                           }, "user")}/>
                                    <Route exact path="/organisations/:organisationKey/users/:userId/consent"
                                           component={props => this.decorate(SampleConsentsPage, {
                                               ...this.props,
                                               organisationKey: props.match.params.organisationKey,
                                               userId: props.match.params.userId,
                                               props
                                           }, "consent")}/>
                                    <Route exact path="/organisations/:organisationKey/consent"
                                           component={props => this.decorate(SampleConsentsPage, {
                                               ...this.props,
                                               organisationKey: props.match.params.organisationKey,
                                               props
                                           }, "consent")}/>
                                    <Route exact path="/consent"
                                           component={props => this.decorate(SampleConsentsPage, {
                                               ...this.props,
                                               props
                                           }, "consent")}/>
                                    <Route exact path="/extractSample"
                                           component={props => this.decorate(UploadFilePage, {
                                               ...this.props,
                                               props
                                           }, "")}/>
                                    <Route exact path="/users"
                                           component={props => this.decorate(UsersPage, {
                                               ...this.props,
                                               props
                                           }, "user")}/>

                                    {
                                        this.props.securityDefault &&
                                        <Route exact path="/accounts"
                                               component={props => this.decorate(AccountsPage, {
                                                   ...this.props,
                                                   props
                                               }, "")}/>
                                    }
                                    {
                                        this.props.securityDefault &&
                                        <Route exact path="/accounts/new"
                                               component={props => this.decorate(AccountPage, {
                                                   ...this.props,
                                                   props
                                               }, "")}/>
                                    }
                                    {
                                        this.props.securityDefault &&
                                        <Route exact path="/accounts/:accountId"
                                               component={props => this.decorate(AccountPage, {
                                                   ...this.props,
                                                   accountId: props.match.params.accountId,
                                                   props
                                               }, "")}/>
                                    }
                                    {
                                        this.props.securityAuth0 &&
                                        <Route exact path="/apiKeys"
                                               component={props => this.decorate(ApiKeysPage, {
                                                   ...this.props,
                                                   props
                                               }, "")}/>
                                    }
                                    {
                                        this.props.securityAuth0 &&
                                        <Route exact path="/apiKeys/new"
                                               component={props => this.decorate(ApiKeyPage, {
                                                   ...this.props,
                                                   props
                                               }, "")}/>
                                    }
                                    {
                                        this.props.securityAuth0 &&
                                        <Route exact path="/apiKeys/:apiKeyId"
                                               component={props => this.decorate(ApiKeyPage, {
                                                   ...this.props,
                                                   apiKeyId: props.match.params.apiKeyId,
                                                   props
                                               }, "")}/>
                                    }
                                </Switch>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        )
    };
}

const NioAppRouter = withRouter(NioApp);

export class RoutedNioApp extends Component {
    render() {
        return (
            <Router basename={`/${this.props.tenant}/bo`}>
                <NioAppRouter {...this.props}/>
            </Router>
        );
    }
}
