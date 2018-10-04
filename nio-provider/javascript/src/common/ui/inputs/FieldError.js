import React from 'react';
import PropTypes from 'prop-types'; // ES6
import * as errorManager from "../../../nio/services/ErrorManagerService";

export const FieldError = (props) => {
  const compare = (message) => {
    if (typeof props.errorKey === 'string') {
      return message === props.errorKey;
    } else {
      return (props.errorKey || []).indexOf(message) !== -1;
    }
  };

  let errors = (props.errorMessage || []).filter(message => compare(message));

  return (
    <div className={`form-group blocFieldError ${errors.length ? "has-error" : ""}`}>
      <div className={`row`}>
        {props.children}

        {
          errors.map((err, index) =>
            <div className="col-xs-12 col-sm-offset-2" key={index}>
              <label className="control-label paddingLabelError">
                {errorManager.translate(err)}
              </label>
            </div>
          )
        }
      </div>
    </div>
  );

};

FieldError.propTypes = {
  errorKey: PropTypes.oneOfType([PropTypes.array, PropTypes.string]),
  errorMessage: PropTypes.array
};
