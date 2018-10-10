import React, {Component} from 'react';

import {BrowserRouter as Router, Route, Link, Switch, withRouter, Redirect} from 'react-router-dom';
import queryString from 'query-string';

import {TopHeader} from "./pages/TopHeader";
import './pages/app.css';
import {UserExtractPage} from "./pages/UserExtractPage";
import {UploadFilePage} from "./pages/UploadFilePage";

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
                <TopHeader userEmail={this.props.userEmail}/>
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
                                    <Route exact path="/" component={props => this.decorate(UserExtractPage, {
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
            <Router basename={`/web`}>
                <NioAppRouter {...this.props}/>
            </Router>
        );
    }
}
