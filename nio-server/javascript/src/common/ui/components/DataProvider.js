import React, { Component } from 'react';

export const DataProvider = (props) => (
  <div className="dataProvider">
    <img src={props.app.logo} width="40" style={{ backgroundColor: 'white' }} />
    <a href={props.app.url} target="_blank" style={{ marginLeft: 10 }}>{props.app.name}</a>
    <div className="btn-group" style={{ marginLeft: 10 }}>
      {props.showLink && <a href={`${props.app.linkUrl}?userId=${props.userId}`} target="_blank" className="btn btn-success btn-xs" >
        <i className="glyphicon glyphicon-link" /> Link
      </a>}
      {props.showUnlink && <a href={`${props.app.unlinkUrl}?userId=${props.userId}`} target="_blank" className="btn btn-warning btn-xs" >
        <i className="glyphicon glyphicon-eject" /> Unlink
      </a>}
    </div>
  </div>
);
