import type { GlobalThemeOverrides } from 'naive-ui'

const themeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: '#00ad4c',
    errorColor: '#FF0055',
    warningColor: '#FF8000',
    borderRadius: '4px',
    borderRadiusSmall: '3px',
    borderColor: '#e4e7ec',
    /** 紧凑布局：全局正文字号 */
    fontSize: '14px',
    fontSizeMini: '12px',
    fontSizeSmall: '12px',
    height: '30px',
    heightSmall: '26px',
  },
  Card: {
    borderRadius: '6px',
    paddingMedium: '12px',
  },
  Tag: {
    borderRadius: '3px',
    fontSizeSmall: '12px',
  },
  List: {
    borderRadius: '0',
    borderColorPopover: '#e4e7ec',
  },
  Notification: {
    padding: '12px',
  },
  Button: {
    heightMedium: '30px',
    heightSmall: '26px',
    fontSizeMedium: '14px',
    paddingMedium: '0 12px',
    paddingSmall: '0 10px',
  },
  Input: {
    heightMedium: '30px',
    fontSizeMedium: '14px',
  },
  InputNumber: {
    buttonHeight: '28px',
  },
  Select: {
    peers: {
      InternalSelection: {
        heightMedium: '30px',
        fontSizeMedium: '14px',
      },
    },
  },
  DataTable: {
    thPaddingMedium: '8px 10px',
    tdPaddingMedium: '6px 10px',
    fontSizeMedium: '12px',
  },
  Form: {
    labelFontSizeTopMedium: '13px',
  },
}

export const darkThemeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: '#00ad4c',
    errorColor: '#FF0055',
    warningColor: '#FF8000',
    borderRadius: '4px',
    borderRadiusSmall: '3px',
    borderColor: '#4b556eff',
    cardColor: '#202c4633',
    popoverColor: '#0f172a',
    modalColor: '#1c202c',
    fontSize: '14px',
    fontSizeMini: '12px',
    fontSizeSmall: '12px',
    height: '30px',
    heightSmall: '26px',
  },
  Card: {
    borderRadius: '6px',
    color: '#1c202c',
    paddingMedium: '12px',
  },
  Dropdown: {
    color: '#1c2334',
  },
  Drawer: {
    color: '#1c202c',
  },
  DataTable: {
    thColor: '#1c202c',
    tdColor: '#1c2334',
    hoverColor: '#1c202c',
    tdColorHover: '#1c202c',
    thPaddingMedium: '8px 10px',
    tdPaddingMedium: '6px 10px',
    fontSizeMedium: '12px',
  },
  Button: {
    heightMedium: '30px',
    heightSmall: '26px',
    fontSizeMedium: '14px',
    paddingMedium: '0 12px',
    paddingSmall: '0 10px',
  },
  Input: {
    heightMedium: '30px',
    fontSizeMedium: '14px',
  },
  InputNumber: {
    buttonHeight: '28px',
  },
  Select: {
    peers: {
      InternalSelection: {
        heightMedium: '30px',
        fontSizeMedium: '14px',
      },
    },
  },
  Tag: {
    borderRadius: '4px',
  },
  List: {
    borderRadius: '0',
    borderColorPopover: '#1c2334',
    colorHoverPopover: '#1c202c',
  },
  Menu: {},
}

export default themeOverrides
