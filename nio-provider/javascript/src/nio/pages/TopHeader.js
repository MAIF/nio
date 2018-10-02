import React, {Component} from 'react';
import PropTypes from "prop-types";
import {Link} from 'react-router-dom';

import './app.css';

export class TopHeader extends Component {

    state = {
        user: ''
    };

    componentDidMount() {
        this.setState({user: this.props.userEmail})
    }

    componentWillReceiveProps(nextProps) {
        this.setState({user: nextProps.userEmail});
    }

    render() {
        return (
            <nav className="navbar navbar-inverse navbar-fixed-top">
                <div className="navbar-header col-sm-2">
                    <button
                        id="toggle-sidebar"
                        type="button"
                        className="navbar-toggle collapsed menu"
                        data-toggle="collapse"
                        data-target="#sidebar"
                        aria-expanded="false"
                        aria-controls="sidebar">
                        <span className="sr-only">Toggle sidebar</span>
                        <span>Menu</span>
                    </button>

                    <Link to={`/`} className="navbar-brand"
                          style={{display: 'flex'}}>仁王&nbsp; Niō Exemple</Link>
                </div>
                <div className="container-fluid">
                    <div id="navbar" className="navbar-collapse collapse">
                        <ul className="nav navbar-nav navbar-right">
                            <li><a href="#">{(this.state.user || "")} <span
                                className="glyphicon glyphicon-off"/></a></li>
                        </ul>
                    </div>
                </div>
            </nav>
        )
    };
}

TopHeader.propTypes = {
    user: PropTypes.string
};
