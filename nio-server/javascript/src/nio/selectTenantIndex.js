import React, {Component} from 'react';

import {BrowserRouter as Router, Route, Link, Switch, withRouter, Redirect} from 'react-router-dom';
import queryString from 'query-string';

import {TopHeader} from "./pages/TopHeader";
import {ConsentsPage, Home} from "./pages/Consents";
import './pages/app.css';
import {OrganisationPage} from "./pages/OrganisationPage";
import {OrganisationsPage} from "./pages/OrganisationsPage";
import {SampleConsentsPage} from "./pages/SampleConsentsPage";
import {OrganisationVersionPage} from "./pages/OrganisationVersionPage";
import {UsersPage} from "./pages/UsersPage";

import {ConsentFactHistoryPage} from "./pages/ConsentFactHistoryPage";
import {ConsentFactPage} from "./pages/ConsentFactPage";
import {SelectInput} from "../common/ui/inputs";
import PropTypes from "prop-types";
import {AccountsPage} from "./pages/AccountsPage";
import {AccountPage} from "./pages/AccountPage";
import {ApiKeysPage} from "./pages/ApiKeysPage";
import {ApiKeyPage} from "./pages/ApiKeyPage";

export class NioTenantApp extends Component {

    state = {
        tenants: [],
        selectedTenant: null
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
        fetch("/api/tenants", {
            method: "GET",
            credentials: 'include',
            headers: {
                Accept: 'application/json',
                'Content-Type': 'application/json'
            }
        })
            .then(r => r.json())
            .then(tenants => this.setState({tenants: tenants.map(t => t.key)}));
    }

    onChange = (value, name) => {
        this.setState({[name]: value});
        window.location = `/${value}/bo`
    };

    render() {
        return (
            <div className="nio-container container-fluid">
                <TopHeader logoutUrl={this.props.logoutUrl} userEmail={this.props.userEmail}
                           accountManagement={this.props.accountManagement}
                           apiKeyManagement={this.props.apiKeyManagement}/>
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
                                </ul>
                            </div>
                            <div className="col-xs-12 col-sm-10 col-sm-offset-2 backoffice-container">
                                <Switch>
                                    <Route exact path="/" component={props => this.decorate(Tenant, {
                                        ...this.props,
                                        props,
                                        selectedTenant: this.state.selectedTenant,
                                        tenants: this.state.tenants,
                                        onChange: this.onChange
                                    }, "")}/>

                                    {
                                        this.props.accountManagement &&
                                        <Route exact path="/accounts"
                                               component={props => this.decorate(AccountsPage, {
                                                   ...this.props,
                                                   props
                                               }, "")}/>
                                    }
                                    {
                                        this.props.accountManagement &&
                                        <Route exact path="/accounts/new"
                                               component={props => this.decorate(AccountPage, {
                                                   ...this.props,
                                                   props
                                               }, "")}/>
                                    }
                                    {
                                        this.props.accountManagement &&
                                        <Route exact path="/accounts/:accountId"
                                               component={props => this.decorate(AccountPage, {
                                                   ...this.props,
                                                   accountId: props.match.params.accountId,
                                                   props
                                               }, "")}/>
                                    }
                                    {
                                        this.props.apiKeyManagement &&
                                        <Route exact path="/apiKeys"
                                               component={props => this.decorate(ApiKeysPage, {
                                                   ...this.props,
                                                   props
                                               }, "")}/>
                                    }
                                    {
                                        this.props.apiKeyManagement &&
                                        <Route exact path="/apiKeys/new"
                                               component={props => this.decorate(ApiKeyPage, {
                                                   ...this.props,
                                                   props
                                               }, "")}/>
                                    }
                                    {
                                        this.props.apiKeyManagement &&
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

export const Tenant = (props) => (

    <div className="row">
        <div className="col-md-12">
            <SelectInput label={"Sélectionnez un tenant"} value={props.selectedTenant}
                         possibleValues={props.tenants}
                         onChange={(value) => props.onChange(value, "selectedTenant")}/>
        </div>
    </div>
);

Tenant.propTypes = {
    selectedTenant: PropTypes.string,
    tenants: PropTypes.array,
    onChange: PropTypes.func
};

const NioTenantAppRouter = withRouter(NioTenantApp);

export class NioTenantAppWithRouter extends Component {
    render() {
        return (
            <Router basename={`/`}>
                <NioTenantAppRouter {...this.props}/>
            </Router>
        );
    }
}
