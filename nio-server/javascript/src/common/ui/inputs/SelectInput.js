import React, {Component} from 'react';
import PropTypes from "prop-types";
import Select from 'react-select';
import {Help} from './Help';
import {FieldError} from "./FieldError";

export class SelectInput extends Component {
  state = {
    loading: false,
    value: this.props.value || null,
    values: (this.props.possibleValues || []).map(a => ({label: a, value: a})),
  };

  componentDidMount() {
    if (this.props.valuesFrom) {
      this.reloadValues();
    }
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.valuesFrom && nextProps.value !== this.props.value) {
      this.reloadValues().then(() => {
        this.setState({value: nextProps.value});
      });
    }
    if (nextProps.possibleValues !== this.props.possibleValues) {
      this.setState({
        values: (nextProps.possibleValues || []).map(a => ({label: a, value: a})),
      });
    }
    if (!nextProps.valuesFrom && nextProps.value !== this.props.value) {
      this.setState({value: nextProps.value});
    }
  }

  reloadValues = () => {
    this.setState({loading: true});
    return fetch(this.props.valuesFrom, {
      method: 'GET',
      credentials: 'include',
      headers: {
        Accept: 'application/json',
      },
    })
      .then(r => r.json())
      .then(values => values.map(this.props.transformer || (a => a)))
      .then(values => this.setState({values, loading: false}));
  };

  onChange = e => {
    this.setState({value: e ? e.value: ''});
    this.props.onChange(e ? e.value: '');
  };

  render() {
    console.log("Value : ", this.state.value, this.state.values);
    return (
      <div className="row selectContent">
        <FieldError errorKey={this.props.errorKey} errorMessage={this.props.errorMessage}>
          <label htmlFor={`input-${this.props.label}`} className="col-xs-12 col-sm-2 control-label">
            {this.props.label} <Help text={this.props.help}/>
          </label>
          <div className="col-sm-10">
            <div style={{width: '100%'}}>
              {!this.props.disabled && (
                <Select
                  style={{width: this.props.more ? '100%' : '100%'}}
                  name={`${this.props.label}-search`}
                  isLoading={this.state.loading}
                  value={this.state.values.filter(
                      v => v.value === this.state.value
                  )}
                  placeholder={this.props.placeholder}
                  options={this.state.values}
                  onChange={this.onChange}
                />
              )}
              {this.props.disabled && (
                <input
                  type="text"
                  className="form-control"
                  disabled={true}
                  placeholder={this.props.placeholder}
                  value={this.state.value}
                  onChange={this.onChange}
                />
              )}
            </div>
          </div>
        </FieldError>
      </div>
    );
  }
}

SelectInput.propTypes = {
  label: PropTypes.string.isRequired,
  disabled: PropTypes.bool,
  valuesFrom: PropTypes.string,
  onChange: PropTypes.func.isRequired,
  possibleValues: PropTypes.array,
  value: PropTypes.any,
  errorKey: PropTypes.oneOfType([PropTypes.array, PropTypes.string]),
  errorMessage: PropTypes.array
};
