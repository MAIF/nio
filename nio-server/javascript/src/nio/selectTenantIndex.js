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

export class NioTenantApp extends Component {

    state = {
        tenants: [],
        selectedTenant: null
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
                <TopHeader logoutUrl={this.props.logoutUrl} userEmail={this.props.userEmail} securityDefault={false} securityAuth0={false}/>
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
                                <div className="row">
                                    <div className="col-md-12">
                                        <SelectInput label={"Sélectionnez un tenant"} value={this.state.selectedTenant}
                                                     possibleValues={this.state.tenants}
                                                     onChange={(value) => this.onChange(value, "selectedTenant")}/>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        )
    };
}

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
