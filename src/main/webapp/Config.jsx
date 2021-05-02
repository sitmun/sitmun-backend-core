import {Table} from "react-bootstrap";
import React from "react";

export function Config({config}) {
  return (
    <Table>
      <thead>
      <tr>
        <th>Property</th>
        <th>Value</th>
      </tr>
      </thead>
      <tbody>
      {
        config.map(value =>
          <tr key={value.name}>
            <td><code>{value.name}</code></td>
            <td><code>{value.value}</code></td>
          </tr>
        )
      }
      </tbody>
    </Table>
  );

}