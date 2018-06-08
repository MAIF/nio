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

export class NioApp extends Component {

  decorate = (Component, props) => {
    const newProps = {...props};
    newProps.location.query = queryString.parse((props.location || {search: ''}).search) || {};
    newProps.params = newProps.match.params || {};
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
        <TopHeader tenant={this.props.tenant} logoutUrl={this.props.logoutUrl} userEmail={this.props.userEmail}/>
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
                      this.props.location.pathname.indexOf('/organisations') !== -1
                      && this.props.location.pathname.indexOf('/users') === -1 ?
                        "active"
                        :
                        ""}>
                    <Link to="/organisations">
                      Organisations
                    </Link>
                  </li>
                  <li className={this.props.location.pathname.indexOf('/users') !== -1 ? "active" : ""}>
                    <Link to="/users">
                      Utilisateurs
                    </Link>
                  </li>
                </ul>
              </div>
              <div className="col-xs-12 col-sm-10 col-sm-offset-2 backoffice-container">
                <Switch>
                  <Route exact path="/" component={props => this.decorate(OrganisationsPage, {
                    ...this.props,
                    props
                  })}/>
                  <Route exact path="/organisations"
                         component={props => this.decorate(OrganisationsPage, {
                           ...this.props,
                           props
                         })}/>
                  <Route exact path="/organisations/new"
                         component={props => this.decorate(OrganisationPage, {
                           ...this.props,
                           onSave: this.goToOrganisations,
                           props
                         })}/>
                  <Route exact path="/organisations/:organisationKey"
                         component={props => this.decorate(OrganisationVersionPage, {
                           ...this.props,
                           organisationKey: props.match.params.organisationKey,
                           onSave: this.goToOrganisations,
                           props
                         })}/>
                  <Route exact path="/organisations/:organisationKey/users"
                         component={props => this.decorate(UsersPage, {
                           ...this.props,
                           organisationKey: props.match.params.organisationKey,
                           props
                         })}/>
                  <Route exact path="/organisations/:organisationKey/users/:userId"
                         component={props => this.decorate(ConsentFactPage, {
                           ...this.props,
                           organisationKey: props.match.params.organisationKey,
                           userId: props.match.params.userId,
                           props
                         })}/>
                  <Route exact path="/consentSample"
                         component={props => this.decorate(SampleConsentsPage, {
                           ...this.props,
                           props
                         })}/>
                  <Route exact path="/users"
                         component={props => this.decorate(UsersPage, {
                           ...this.props,
                           props
                         })}/>
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
