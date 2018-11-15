import React, {Component} from 'react';
import PropTypes from "prop-types";
import {TextInput} from "../../common/ui/inputs";

export class NioLoginPage extends Component {

    state = {
        email: '',
        password: '',
        error: false
    };

    onChange = (value, name) => {
        this.setState({[name]: value})
    };

    clean = () => {
        this.setState({email: '', password: '', error: false})
    };

    login = () => {
        fetch(`/api/nio/login`, {
            method: "POST",
            credentials: 'include',
            headers: {
                Accept: 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                email: this.state.email,
                password: this.state.password
            })
        }).then(r => {
            if (r.status === 200) {
                window.location = "/";
            } else {
                this.setState({error: true})
            }
        });
    };

    render() {
        return (
            <div className="container">
                <div className="jumbotron">
                    <h3>Login to Niō</h3>
                      <div className="form-horizontal col-md-12" style={{marginTop: "20px"}}>
                          <TextInput label={"email"} value={this.state.email} onChange={(v) => this.onChange(v, "email")}/>
                          <TextInput label={"password"} value={this.state.password}
                                     onChange={(v) => this.onChange(v, "password")} type={"password"}/>
                          {
                              this.state.error &&
                              <p className="text-danger">Invalid login or password</p>
                          }

                          <div className="form-buttons pull-right">
                              <button type="button" className="btn btn-danger" onClick={this.clean}>
                                  Cancel
                              </button>
                              <button type="button" className="btn btn-success" onClick={this.login}>
                                  <i className="glyphicon glyphicon-hdd"/> Login
                              </button>
                          </div>
                      </div>
                  <img className="logo_izanami_dashboard" style={{width:'300px'}} src={`/assets/images/opun-nio.png`}/>
              </div>
            </div>
        )
    }
}

NioLoginPage.propTypes = {};
